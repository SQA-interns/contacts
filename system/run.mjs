#!/usr/bin/env node
// Runner for the conductor (/run). Deterministic and agent-agnostic: it reads
// the handover in 02_output/docs/00_progress.md, starts the next session as a
// separate non-interactive process of the agent named for that session in
// run-config.md (Claude Code, Codex, a local model, any command template), and
// runs the hidden evaluation itself, so no agent ever sees the hidden suite.
//
// Usage:
//   node system/run.mjs --status              report the next step, run nothing
//   node system/run.mjs --once                run the next session, then report
//   node system/run.mjs --once --command "/clarify D-03: 2"   run this command instead
//   node system/run.mjs --vault               move the hidden suite out of the project
//   node system/run.mjs --hidden-eval         run the hidden suite on a copy, write results
//   node system/run.mjs --restore-hidden      copy the hidden suite back (before /release)
//
// The last line of output is always: RUN_STATUS {json}
// status: ready | done | waiting | interactive | failed | complete
// No dependencies; Node 18 or newer.

import {
  readFileSync, writeFileSync, appendFileSync, mkdirSync, existsSync, readdirSync,
  statSync, cpSync, rmSync, mkdtempSync,
} from "node:fs";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { join, dirname, basename, relative, resolve } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const DOCS = join(ROOT, "02_output/docs");
const PROGRESS = join(DOCS, "00_progress.md");
const DECISIONS = join(DOCS, "decisions-log.md");
const HIDDEN_MANIFEST = join(DOCS, "03_hidden-manifest.sha256");
const HIDDEN_RESULTS = join(DOCS, "06_hidden-evaluation.md");
const HIDDEN_DIR = join(ROOT, "02_output/hidden-tests");
const RUN_CONFIG = join(ROOT, "01_input/01_project/00_setup/run-config.md");
const LOG_DIR = join(ROOT, "02_output/logs/run");

// Sessions that are a conversation with the developer and cannot run headless.
const INTERACTIVE = new Set(["/grill-me", "/design"]);

// Tools every Claude session may use without a prompt; project commands come
// from run-config.md ("Allowed commands").
const BASE_TOOLS = [
  "Read", "Edit", "Write", "Glob", "Grep",
  "Bash(git *)", "Bash(ls *)", "Bash(mkdir *)", "Bash(find *)", "Bash(grep *)",
  "Bash(sed -n *)", "Bash(wc *)", "Bash(sha256sum *)", "Bash(shasum *)",
  "Bash(node *)", "Bash(npm *)", "Bash(npx *)", "Bash(docker *)",
];

// Used when run-config.md has no "Agents" table.
const DEFAULT_AGENTS = {
  claude: { invocation: "slash", command: "claude -p {prompt} --model {model} --effort {effort} --permission-mode {permission} --output-format json --allowedTools {allowed}" },
};

// ---------- reading the handover and the configuration ----------

const read = (p) => (existsSync(p) ? readFileSync(p, "utf8") : "");

function field(text, name) {
  const m = text.match(new RegExp(`^- ${name}:[ \\t]*(.*)$`, "m"));
  return m ? m[1].trim() : "";
}

function section(text, heading) {
  const start = text.indexOf(`## ${heading}`);
  if (start < 0) return "";
  const rest = text.slice(start + heading.length + 3);
  const end = rest.search(/^## /m);
  return end < 0 ? rest : rest.slice(0, end);
}

// Rows of every Markdown table in `text` whose header contains all `need` columns.
function table(text, need) {
  const lines = text.split("\n");
  for (let i = 0; i < lines.length - 1; i++) {
    const head = cells(lines[i]);
    if (!need.every((n) => head.includes(n)) || !/^\|[\s|:-]+\|$/.test(lines[i + 1].trim())) continue;
    const rows = [];
    for (let j = i + 2; j < lines.length && lines[j].trim().startsWith("|"); j++) {
      const c = cells(lines[j]);
      rows.push(Object.fromEntries(head.map((h, k) => [h, c[k] ?? ""])));
    }
    return rows;
  }
  return [];
}

function cells(line) {
  const t = line.trim();
  if (!t.startsWith("|")) return [];
  return t.slice(1, t.endsWith("|") ? -1 : undefined).split("|").map((c) => c.trim());
}

const unquote = (s) => s.replace(/^`|`$/g, "");

function progress() {
  const text = read(PROGRESS);
  const last = section(text, "Last session");
  const next = section(text, "Next");
  return {
    lastBlock: last.trim(),
    lastSession: field(last, "Session"),
    result: field(last, "Result"),
    gate: field(last, "Gate"),
    command: field(next, "Command"),
    waiting: field(next, "Waiting for"),
  };
}

function runConfig() {
  const text = read(RUN_CONFIG);
  const defaultAgent = field(text, "Agent") || "claude";
  const sessions = {};
  for (const row of table(text, ["Skill", "Model", "Effort"])) {
    for (const m of row.Skill.matchAll(/`(\/[a-z-]+)`/g)) {
      sessions[m[1]] = { agent: unquote(row.Agent || "") || defaultAgent, model: unquote(row.Model), effort: unquote(row.Effort), maxTurns: unquote(row["Max turns"] || "") };
    }
  }
  const agents = { ...DEFAULT_AGENTS };
  for (const row of table(text, ["Agent", "Invocation", "Command"])) {
    agents[unquote(row.Agent)] = { invocation: unquote(row.Invocation), command: unquote(row.Command) };
  }
  const allowed = section(text, "Allowed commands").split("\n")
    .map((l) => l.match(/^- `(.+)`/)).filter(Boolean).map((m) => m[1]).filter((r) => !r.includes("<"));
  const vault = field(text, "Hidden vault") || `../${basename(ROOT)}.hidden-vault`;
  return { sessions, agents, allowed, permission: field(text, "Permission mode") || "default", vault: resolve(ROOT, vault) };
}

function decisionsWaiting(waiting) {
  const log = read(DECISIONS);
  return (waiting.match(/D-\d+/g) || []).map((id) => {
    const start = log.indexOf(`## ${id}:`);
    if (start < 0) return { id, text: "(not found in decisions-log.md)" };
    const rest = log.slice(start);
    const end = rest.slice(3).search(/^## D-\d+:/m);
    return { id, text: (end < 0 ? rest : rest.slice(0, end + 3)).trim() };
  });
}

function report(status, extra = {}) {
  const p = progress();
  console.log("RUN_STATUS " + JSON.stringify({
    status, last: p.lastSession, result: p.result, gate: p.gate, next: p.command, waiting: p.waiting, ...extra,
  }));
  process.exit(status === "failed" ? 1 : 0);
}

// ---------- starting a session through an agent adapter ----------

// Split a command template into words, honouring "double" and 'single' quotes.
function words(template) {
  const out = [];
  for (const m of template.matchAll(/"([^"]*)"|'([^']*)'|(\S+)/g)) out.push(m[1] ?? m[2] ?? m[3]);
  return out;
}

function prompt(command, invocation) {
  if (invocation === "slash") return command;
  if (invocation === "dollar") return "$" + command.slice(1);
  const [skill, ...rest] = command.split(/\s+/);
  const args = rest.join(" ");
  return `Read AGENTS.md first. Then follow the skill .claude/skills/${skill.slice(1)}/SKILL.md exactly` +
    (args ? `, with the argument: ${args}` : "") + ". Do only that session's work, then stop.";
}

function buildArgs(agent, row, command, cfg) {
  const values = {
    "{prompt}": [prompt(command, agent.invocation)], "{model}": [row.model], "{effort}": [row.effort],
    "{permission}": [cfg.permission], "{allowed}": [...BASE_TOOLS, ...cfg.allowed],
    "{limits}": row.maxTurns ? ["--max-turns", row.maxTurns] : [],
  };
  const out = [];
  for (const w of words(agent.command)) {
    if (values[w]) { out.push(...values[w]); continue; }
    out.push(w.replace(/\{(model|effort|permission)\}/g, (_, k) => values[`{${k}}`][0]));
  }
  return out;
}

function nextLogFile(skill) {
  mkdirSync(LOG_DIR, { recursive: true });
  const n = readdirSync(LOG_DIR).filter((f) => f.endsWith(".json")).length + 1;
  return join(LOG_DIR, `${String(n).padStart(3, "0")}-${skill.slice(1)}.json`);
}

function runSession(command) {
  const skill = command.split(/\s+/)[0];
  const cfg = runConfig();
  const row = cfg.sessions[skill];
  if (!row) return { ok: false, error: `no row for ${skill} in run-config.md` };
  const agent = cfg.agents[row.agent];
  if (!agent) return { ok: false, error: `no adapter for agent "${row.agent}" in run-config.md` };
  const [bin, ...args] = buildArgs(agent, row, command, cfg);
  const res = spawnSync(bin, args, { cwd: ROOT, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  const logFile = nextLogFile(skill);
  let parsed = null;
  try { parsed = JSON.parse(res.stdout); } catch { /* not JSON: keep the raw text */ }
  writeFileSync(logFile, JSON.stringify({
    command, agent: row.agent, model: row.model, effort: row.effort, exitCode: res.status,
    error: res.error ? String(res.error) : undefined,
    session_id: parsed?.session_id, total_cost_usd: parsed?.total_cost_usd,
    result: parsed ? parsed.result : res.stdout, stderr: res.stderr,
  }, null, 2));
  return { ok: res.status === 0 && !res.error, logFile, error: res.error ? String(res.error) : res.status ? `exit code ${res.status}` : "" };
}

// ---------- hidden evaluation (never shown to an agent) ----------

function files(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) out.push(...files(p)); else out.push(p);
  }
  return out;
}

const lfHash = (p) => createHash("sha256").update(readFileSync(p).toString("binary").replace(/\r\n/g, "\n"), "binary").digest("hex");

function vault() {
  const cfg = runConfig();
  if (!existsSync(HIDDEN_DIR)) return existsSync(cfg.vault) ? { ok: true, note: "already in the vault" } : { ok: false, error: "no hidden suite in 02_output/hidden-tests/" };
  if (!existsSync(join(HIDDEN_DIR, "hidden.json"))) return { ok: false, error: "02_output/hidden-tests/hidden.json is missing" };
  rmSync(cfg.vault, { recursive: true, force: true });
  cpSync(HIDDEN_DIR, cfg.vault, { recursive: true });
  rmSync(HIDDEN_DIR, { recursive: true, force: true });
  return { ok: true, note: `hidden suite moved to ${cfg.vault}` };
}

function manifestCheck(dir) {
  const lines = read(HIDDEN_MANIFEST).split("\n").filter((l) => /^[0-9a-f]{64}\s+/.test(l));
  if (!lines.length) return ["docs/03_hidden-manifest.sha256 is empty or missing"];
  const bad = [];
  for (const line of lines) {
    const [hash, ...rest] = line.split(/\s+/);
    const rel = rest.join(" ").replace(/^\*/, "").replace(/^hidden-tests\//, "");
    const p = join(dir, rel);
    if (!existsSync(p)) bad.push(`missing: ${rel}`); else if (lfHash(p) !== hash) bad.push(`changed: ${rel}`);
  }
  return bad;
}

function junitCases(xml) {
  const out = [];
  for (const m of xml.matchAll(/<testcase\b([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const name = (m[1].match(/\bname="([^"]*)"/) || [])[1] || "";
    const body = m[3] || "";
    out.push({ name, failed: /<(failure|error|skipped)\b/.test(body) });
  }
  return out;
}

function hiddenEval() {
  const cfg = runConfig();
  if (existsSync(HIDDEN_DIR)) { const v = vault(); if (!v.ok) return v; }
  if (!existsSync(join(cfg.vault, "hidden.json"))) return { ok: false, error: `no hidden suite in ${cfg.vault}` };
  const spec = JSON.parse(read(join(cfg.vault, "hidden.json")));
  const head = spawnSync("git", ["rev-parse", "--short", "HEAD"], { cwd: ROOT, encoding: "utf8" }).stdout.trim() || "unknown";
  const bad = manifestCheck(cfg.vault);
  const work = mkdtempSync(join(tmpdir(), "hidden-eval-"));
  let cases = [], exitCode = null, error = "";
  try {
    if (!bad.length) {
      cpSync(ROOT, work, { recursive: true, filter: (src) => !relative(ROOT, src).split(/[\\/]/).includes(".git") });
      cpSync(cfg.vault, join(work, "02_output/hidden-tests"), { recursive: true });
      const res = spawnSync(spec.command, { cwd: work, shell: true, encoding: "utf8", maxBuffer: 64 * 1024 * 1024, timeout: 60 * 60 * 1000 });
      exitCode = res.status;
      const junit = join(work, spec.junit);
      if (existsSync(junit)) cases = junitCases(read(junit)); else error = `JUnit report ${spec.junit} was not written`;
    }
  } finally {
    rmSync(work, { recursive: true, force: true });
  }
  const failed = cases.filter((c) => c.failed).map((c) => {
    const rule = (c.name.match(/\b(?:R-\d{3}-\d{2}|BR-\d{2})\b/) || ["unknown rule"])[0];
    const category = (c.name.match(/boundary|combination|invalid|role/i) || ["other"])[0].toLowerCase();
    return { rule, category };
  });
  const runs = (read(HIDDEN_RESULTS).match(/^## Run \d+/gm) || []).length;
  if (!existsSync(HIDDEN_RESULTS)) {
    writeFileSync(HIDDEN_RESULTS, "# Hidden evaluation\n\n> For: agent, human · Written by: `system/run.mjs --hidden-eval` · Agent: read-only\n\nResults of the held-out suite, run by the runner on a copy of the project. Failures name only the rule and the case category, never inputs, expected values or test code.\n");
  }
  const lines = [
    "", `## Run ${runs + 1}`, "",
    `- Commit: ${head}`, `- Date: ${new Date().toISOString()}`,
    `- Manifest: ${bad.length ? "MISMATCH (" + bad.join("; ") + ")" : "ok"}`,
    `- Tests: ${cases.length}, passed: ${cases.length - failed.length}, failed: ${failed.length}`,
    `- Exit code: ${exitCode ?? "not run"}${error ? `; ${error}` : ""}`, "",
    "| Rule | Case category |", "|---|---|",
    ...failed.map((f) => `| ${f.rule} | ${f.category} |`), "",
  ];
  appendFileSync(HIDDEN_RESULTS, lines.join("\n"));
  return { ok: !bad.length && !error, note: `hidden suite: ${cases.length - failed.length}/${cases.length} passed`, error: bad.length ? "manifest mismatch" : error };
}

function restoreHidden() {
  const cfg = runConfig();
  if (!existsSync(cfg.vault)) return { ok: existsSync(HIDDEN_DIR), note: "nothing to restore", error: "no vault" };
  cpSync(cfg.vault, HIDDEN_DIR, { recursive: true });
  return { ok: true, note: "hidden suite copied back to 02_output/hidden-tests/" };
}

// ---------- run summary (numbers for the retrospective) ----------

function summary() {
  const logs = existsSync(LOG_DIR) ? readdirSync(LOG_DIR).filter((f) => f.endsWith(".json")) : [];
  let cost = 0, failed = 0;
  const bySkill = {};
  for (const f of logs) {
    const j = JSON.parse(read(join(LOG_DIR, f)));
    const skill = (j.command || "").split(/\s+/)[0];
    bySkill[skill] = (bySkill[skill] || 0) + 1;
    if (typeof j.total_cost_usd === "number") cost += j.total_cost_usd;
    if (j.exitCode) failed += 1;
  }
  const dlog = read(DECISIONS);
  const count = (re) => (dlog.match(re) || []).length;
  const verifyRuns = (read(join(DOCS, "06_verification-report.md")).match(/^## Run \d+/gm) || []).length;
  const commits = spawnSync("git", ["rev-list", "--count", "HEAD"], { cwd: ROOT, encoding: "utf8" }).stdout.trim() || "0";
  const rows = [
    ["Sessions started", String(logs.length)], ["Sessions failed (exit code)", String(failed)],
    ["Sessions per skill", Object.entries(bySkill).map(([k, v]) => `${k} ${v}`).join(", ") || "none"],
    ["Cost reported by the agents (USD)", cost ? cost.toFixed(2) : "not reported"],
    ["Decisions", String(count(/^## D-\d+:/gm))],
    ["Decisions by class (allow / open / block)", `${count(/^- Class: allow/gm)} / ${count(/^- Class: open/gm)} / ${count(/^- Class: block/gm)}`],
    ["Approvals", String(count(/^- Type: approval/gm))],
    ["Verification runs", String(verifyRuns)], ["Commits", commits],
  ];
  const block = "## Numbers\n\nWritten by `system/run.mjs --summary`.\n\n| Measure | Value |\n|---|---|\n" + rows.map(([a, b]) => `| ${a} | ${b} |`).join("\n") + "\n";
  const file = join(DOCS, "07_retrospective.md");
  const text = read(file);
  const start = text.indexOf("## Numbers");
  const next = start < 0 ? -1 : text.indexOf("\n## ", start + 3);
  const out = start < 0 ? text.trimEnd() + "\n\n" + block : text.slice(0, start) + block + (next < 0 ? "" : text.slice(next + 1));
  writeFileSync(file, out);
  return { ok: true, note: rows.map(([a, b]) => `${a}: ${b}`).join("; ") };
}

// ---------- template lint (structure, duplication and reference checks) ----------

function lintTemplate() {
  const errors = [];
  const skip = (rel) => /^(\.git|\.agents|02_output\/(logs|hidden-tests)|node_modules|01_input\/01_project\/00_intake\/customer_requirement)(\/|$)/.test(rel); // customer files are not template documents
  const all = [];
  (function walk(dir) {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name), rel = relative(ROOT, p);
      if (skip(rel)) continue;
      if (statSync(p).isDirectory()) walk(p); else all.push(rel);
    }
  })(ROOT);
  const md = all.filter((f) => f.endsWith(".md"));
  const alias = { "general/": "01_input/00_general/", "project/": "01_input/01_project/", "skills/": ".claude/skills/", "docs/": "02_output/docs/", "out/": "02_output/" };
  const agentMade = /(manifest\.sha256|02_adr\/|02_contracts\/|logs\/|US-nnn|06_hidden-evaluation\.md|wireframe\.css|WF-nn)/;
  const defined = {};
  for (const f of md) {
    const text = read(join(ROOT, f));
    const lines = text.split("\n");
    if (lines.length > 200) errors.push(`${f}: ${lines.length} lines (max 200)`);
    if (!/^> For: /m.test(text)) errors.push(`${f}: no "> For:" header line`);
    for (const m of text.matchAll(/`((?:general|project|skills|docs|out)\/[A-Za-z0-9_./-]+)`/g)) {
      const ref = m[1];
      if (agentMade.test(ref) || ref.includes("*") || ref.includes("<")) continue;
      const key = Object.keys(alias).find((k) => ref.startsWith(k));
      if (!existsSync(join(ROOT, alias[key] + ref.slice(key.length)))) errors.push(`${f}: reference ${ref} does not exist`);
    }
    if (f.startsWith("01_input/00_general/")) {
      for (const m of text.matchAll(/^\| ((?:UI|ES|SB|DoR-[SR]|DoD-[SIR])-\d+) \|/gm)) (defined[m[1]] ||= []).push(f);
    }
    if (f.startsWith(".claude/skills/")) {
      const reads = (text.match(/^- Reads: .*$/m) || [""])[0];
      if (/(^|[\s`])(README\.md|Run_Guide\.md|system\/)/.test(reads)) errors.push(`${f}: "Reads" names a human or system file`);
    }
  }
  for (const [id, files] of Object.entries(defined)) if (files.length > 1) errors.push(`${id} is defined ${files.length} times (${files.join(", ")})`);
  const cfg = runConfig();
  for (const dir of readdirSync(join(ROOT, ".claude/skills"))) {
    if (["build-ui", "grill-me", "run"].includes(dir)) continue; // not started by the runner
    if (!cfg.sessions[`/${dir}`]) errors.push(`skill /${dir} has no row in run-config.md ("Sessions")`);
  }
  return errors.length ? { ok: false, error: `${errors.length} problem(s): ` + errors.join(" | ") } : { ok: true, note: `template ok: ${md.length} Markdown files checked` };
}

// ---------- main ----------

function main() {
  const argv = process.argv.slice(2);
  const has = (f) => argv.includes(f);
  const ci = argv.indexOf("--command");
  const override = ci >= 0 ? argv[ci + 1] : "";

  for (const [flag, fn] of [["--vault", vault], ["--hidden-eval", hiddenEval], ["--restore-hidden", restoreHidden], ["--summary", summary], ["--lint-template", lintTemplate]]) {
    if (has(flag)) { const r = fn(); report(r.ok ? "done" : "failed", { step: flag, note: r.note, error: r.error }); }
  }

  const before = progress();
  if (!override && (/complete/i.test(before.result) || /^(none|-|)$/i.test(before.command))) report("complete");
  if (!override && before.waiting && !/^nothing$/i.test(before.waiting)) {
    report("waiting", { decisions: decisionsWaiting(before.waiting) });
  }
  const command = override || before.command;
  const skill = command.split(/\s+/)[0];
  const designAsk = !/^-\s*Design round:\s*skip/im.test(read(RUN_CONFIG));
  if (INTERACTIVE.has(skill) && (skill !== "/design" || designAsk)) report("interactive", { command });
  if (!has("--once")) report("ready", { command });

  // Deterministic steps around the sessions: the hidden suite never stays in the project.
  if (skill === "/verify") { const r = hiddenEval(); if (!r.ok) report("failed", { step: "--hidden-eval", error: r.error }); }
  if (skill === "/release") restoreHidden();

  let outcome;
  for (let attempt = 1; attempt <= 2; attempt++) {
    // A retry tells the session to continue where the failed one stopped (AGENTS.md, "Resume").
    outcome = runSession(attempt === 1 ? command : `${command} resume`);
    const after = progress();
    if (outcome.ok && after.lastBlock !== before.lastBlock) {
      if (skill === "/acceptance-tests") { const v = vault(); if (!v.ok) report("failed", { step: "--vault", error: v.error, log: outcome.logFile }); }
      if (/complete/i.test(after.result)) report("complete", { log: outcome.logFile });
      if (after.waiting && !/^nothing$/i.test(after.waiting)) report("waiting", { log: outcome.logFile, decisions: decisionsWaiting(after.waiting) });
      if (/failed/i.test(after.result)) report("failed", { log: outcome.logFile, attempt });
      report("done", { log: outcome.logFile });
    }
  }
  report("failed", { log: outcome?.logFile, error: outcome?.error || "the session ended without updating 00_progress.md", attempts: 2 });
}

main();
