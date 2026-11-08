#!/usr/bin/env node
// Fails when `npm audit` reports a High or Critical package that is not an accepted finding (SB-08, DoD-P06).
// Usage: node scripts/npm-audit-gate.mjs <dir> [--omit=dev]
// Accepted findings: decision D-05 (human, 2026-11-08): the firebase family; Firestore and gRPC are not used
// (Authentication only). A new package in this list needs a new decision.
import { execFileSync } from 'node:child_process';

const accepted = new Set(['firebase', '@firebase/firestore', '@firebase/firestore-compat', '@grpc/grpc-js']);
const [dir, ...flags] = process.argv.slice(2);
if (!dir) throw new Error('usage: npm-audit-gate.mjs <dir> [--omit=dev]');

let output;
try {
  output = execFileSync('npm', ['audit', '--json', ...flags], { cwd: dir, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
} catch (error) {
  output = error.stdout; // npm audit exits non-zero when it finds something
}
const report = JSON.parse(output);
const blocking = Object.entries(report.vulnerabilities ?? {}).filter(
  ([name, v]) => ['high', 'critical'].includes(v.severity) && !accepted.has(name),
);
const counts = report.metadata?.vulnerabilities ?? {};
console.log(`npm audit ${dir} ${flags.join(' ')}: critical ${counts.critical ?? 0}, high ${counts.high ?? 0}, moderate ${counts.moderate ?? 0}, low ${counts.low ?? 0}`);
if (blocking.length > 0) {
  console.log('NOT accepted:', blocking.map(([name, v]) => `${name} (${v.severity})`).join(', '));
  process.exit(1);
}
console.log('only findings accepted in D-05 remain');
