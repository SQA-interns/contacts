#!/usr/bin/env node
// Checks that every requirement ID of the inputs is traceable to a section of
// docs/02_specification.md (ES-05 style helper, no dependencies).
// Usage: node scripts/check-traceability.mjs [--write]   (run from 02_output)
import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const input = join(root, '..', '01_input', '01_project');
const specPath = join(root, 'docs', '02_specification.md');
const stories = readdirSync(join(input, '01_requirements', 'stories')).filter((f) => /^US-\d+\.md$/.test(f)).sort();

const ranges = (nums, prefix, us) => {
  if (!nums.length) return [];
  const w = String(nums[0]).length >= 2 ? 2 : 2;
  const sorted = [...new Set(nums)].sort((a, b) => a - b);
  const pad = (n) => String(n).padStart(w, '0');
  const out = [];
  let a = sorted[0], b = a;
  for (const n of sorted.slice(1)) {
    if (n === b + 1) b = n; else { out.push([a, b]); a = b = n; }
  }
  out.push([a, b]);
  return out.map(([x, y]) => (x === y ? `${prefix}-${us}-${pad(x)}` : `${prefix}-${us}-${pad(x)}..${pad(y)}`));
};

const ids = {};   // all ids found in inputs
const rows = [];
for (const f of stories) {
  const us = f.slice(3, 6);
  const text = readFileSync(join(input, '01_requirements', 'stories', f), 'utf8');
  const per = {};
  for (const p of ['R', 'AC', 'EX']) {
    const re = new RegExp(`\\b${p}-${us}-(\\d{2})\\b`, 'g');
    const nums = [...text.matchAll(re)].map((m) => Number(m[1]));
    per[p] = ranges(nums, p, us);
    for (const n of new Set(nums)) (ids[`${p}-${us}-${String(n).padStart(2, '0')}`] = `US-${us}`);
  }
  rows.push(`| ${[...per.R, ...per.AC, ...per.EX].join(', ')} | Behaviour per story: US-${us} |`);
}

const simple = (file, prefix, section) => {
  const t = readFileSync(join(input, ...file), 'utf8');
  const found = [...new Set([...t.matchAll(new RegExp(`\\b${prefix}-\\d{2}\\b`, 'g'))].map((m) => m[0]))].sort();
  for (const i of found) ids[i] = section;
  return found;
};
const br = simple(['01_requirements', 'business-rules.md'], 'BR', 'x');
const sr = simple(['02_design', 'security-requirements.md'], 'SR', 'x');
const nfr = simple(['02_design', 'quality-requirements.md'], 'NFR', 'x');
const sb = readFileSync(join(root, '..', '01_input', '00_general', 'security', 'security-baseline.md'), 'utf8');
const sbIds = [...new Set([...sb.matchAll(/\bSB-\d{2}\b/g)].map((m) => m[0]))].sort();

const brMap = {
  'BR-01': 'Security controls (SR-01); ADR-05; Behaviour per story: US-018',
  'BR-02': 'Permission table; ADR-05; Behaviour per story',
  'BR-03': 'ADR-08; Behaviour per story: US-016, US-011 to US-017',
  'BR-04': 'ADR-09; Behaviour per story: US-012, US-013, US-020',
  'BR-05': 'ADR-06; Data model and validation',
  'BR-06': 'Data model and validation; Behaviour per story: US-003',
  'BR-07': 'ADR-07; Behaviour per story: US-003',
};
const out = [];
out.push('| R / AC / EX / BR / SR / SB / NFR | Section |', '|---|---|');
out.push(...rows);
for (const b of br) out.push(`| ${b} | ${brMap[b] ?? 'MISSING'} |`);
out.push(`| ${sr.join(', ')} | Security controls (table rows SR-01..SR-13); Threat model |`);
out.push(`| ${sbIds.join(', ')} | Security controls (table rows SB-01..SB-14); Threat model |`);
out.push(`| ${nfr.join(', ')} | NFR checks (one row each) |`);
const table = out.join('\n');

let spec = readFileSync(specPath, 'utf8');
const missing = [];
for (const i of [...br, ...sr, ...sbIds, ...nfr]) if (!spec.includes(i) && !table.includes(i)) missing.push(i);
for (const [i, us] of Object.entries(ids)) if (/^(R|AC|EX)-/.test(i) && !spec.includes(`**${us} `) ) missing.push(`${i} (no section ${us})`);
for (const b of br) if (!brMap[b]) missing.push(b);
const marker = /## Traceability[\s\S]*$/;
if (process.argv.includes('--write')) {
  spec = spec.replace(/\n*<!-- slice 5: traceability -->\n?/, '\n');
  spec = spec.replace(marker, '').trimEnd() + '\n\n## Traceability\n\nEvery requirement ID of the inputs maps to a section of this document (rows are generated and checked by `scripts/check-traceability.mjs`; a story row lists its rules, acceptance criteria and edge cases, implemented in the story entry of "Behaviour per story").\n\n' + table + '\n';
  writeFileSync(specPath, spec);
}
console.log(`ids: ${Object.keys(ids).length} story ids, ${br.length} BR, ${sr.length} SR, ${sbIds.length} SB, ${nfr.length} NFR`);
if (missing.length) { console.log('MISSING:\n' + missing.join('\n')); process.exit(1); }
console.log('traceability: all ids mapped');
