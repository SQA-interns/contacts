// Merges the JUnit XML files of the hidden suite (failsafe reports and the Playwright report) into one file.
// Usage: node merge-junit.mjs <out.xml> <dir-or-file>...
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const [out, ...inputs] = process.argv.slice(2);
const files = [];
for (const input of inputs) {
  if (!existsSync(input)) continue;
  if (statSync(input).isDirectory()) {
    for (const f of readdirSync(input)) if (/^TEST-.*\.xml$/.test(f)) files.push(join(input, f));
  } else files.push(input);
}

const suites = [];
let tests = 0;
let failures = 0;
let errors = 0;
let skipped = 0;
for (const file of files) {
  const xml = readFileSync(file, 'utf8').replace(/<\?xml[^>]*\?>/, '');
  for (const m of xml.matchAll(/<testsuite\b[\s\S]*?<\/testsuite>/g)) {
    const suite = m[0];
    suites.push(suite);
    const attr = (name) => Number((suite.match(new RegExp(`<testsuite\\b[^>]*\\b${name}="(\\d+)"`)) ?? [])[1] ?? 0);
    tests += attr('tests');
    failures += attr('failures');
    errors += attr('errors');
    skipped += attr('skipped');
  }
}
writeFileSync(
  out,
  `<?xml version="1.0" encoding="UTF-8"?>\n<testsuites name="hidden" tests="${tests}" failures="${failures}" errors="${errors}" skipped="${skipped}">\n${suites.join('\n')}\n</testsuites>\n`,
);
console.log(`hidden suite: ${tests} tests, ${failures} failures, ${errors} errors -> ${out}`);
process.exitCode = tests > 0 && failures + errors === 0 ? 0 : 1;
