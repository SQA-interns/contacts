// Writes docs/openapi.json, the OpenAPI description delivered to the customer (NFR-15, T-76), from the
// contract docs/02_contracts/openapi.yaml. Run `npm run export-openapi` after every change of the contract;
// `npm run export-openapi -- --check` exits 1 when docs/openapi.json is out of date.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import YAML from 'yaml';

const docs = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs');
const json = `${JSON.stringify(YAML.parse(readFileSync(join(docs, '02_contracts', 'openapi.yaml'), 'utf8')), null, 2)}\n`;
const target = join(docs, 'openapi.json');

if (process.argv.includes('--check')) {
  const current = existsSync(target) ? readFileSync(target, 'utf8') : '';
  if (current !== json) {
    console.log('FAIL docs/openapi.json is out of date; run `npm run export-openapi` in 02_output/tools');
    process.exit(1);
  }
  console.log('ok   docs/openapi.json matches the contract');
} else {
  writeFileSync(target, json);
  console.log(`written ${target}`);
}
