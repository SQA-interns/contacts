// Validates every contract in ../docs/02_contracts with a parser (phase 2 gate).
// OpenAPI: schema validation + project rules. JSON Schema: compile + validate its own `examples`.
import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import YAML from 'yaml';
import { Validator } from '@seriousme/openapi-schema-validator';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const dir = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs', '02_contracts');
let failed = 0;
const fail = (msg) => { failed += 1; console.log(`FAIL ${msg}`); };
const ok = (msg) => console.log(`ok   ${msg}`);

for (const file of readdirSync(dir).sort()) {
  const path = join(dir, file);
  if (file.endsWith('.yaml')) {
    const doc = YAML.parse(readFileSync(path, 'utf8'));
    const res = await new Validator().validate(doc);
    if (!res.valid) { fail(`${file}: ${JSON.stringify(res.errors)}`); continue; }
    ok(`${file}: valid OpenAPI ${doc.openapi}`);
    let ops = 0;
    const ids = new Set();
    for (const [p, item] of Object.entries(doc.paths ?? {})) {
      for (const [m, op] of Object.entries(item)) {
        if (!['get', 'post', 'put', 'patch', 'delete'].includes(m)) continue;
        ops += 1;
        if (!op.operationId) fail(`${file}: ${m} ${p} has no operationId`);
        else if (ids.has(op.operationId)) fail(`${file}: duplicate operationId ${op.operationId}`);
        else ids.add(op.operationId);
        const groupScoped = p.startsWith('/groups/{groupId}');
        if (groupScoped && !op['x-required-action']) fail(`${file}: ${m} ${p} has no x-required-action`);
        if (p !== '/health' && !op.security && !doc.security) fail(`${file}: ${m} ${p} has no security`);
        if (!op.responses?.['401'] && p !== '/health') fail(`${file}: ${m} ${p} lacks a 401 response`);
        if (groupScoped && !op.responses?.['404']) fail(`${file}: ${m} ${p} lacks a 404 response`);
      }
    }
    ok(`${file}: ${ops} operations, project rules checked`);
  } else if (file.endsWith('.schema.json')) {
    const schema = JSON.parse(readFileSync(path, 'utf8'));
    const ajv = new Ajv2020({ strict: true, strictTuples: false, allErrors: true });
    addFormats(ajv);
    ajv.addKeyword('x-invalid-examples');
    let validate;
    try { validate = ajv.compile(schema); } catch (e) { fail(`${file}: ${e.message}`); continue; }
    const examples = schema.examples ?? [];
    if (examples.length === 0) fail(`${file}: no examples to validate`);
    examples.forEach((ex, i) => { if (!validate(ex)) fail(`${file}: example ${i} ${ajv.errorsText(validate.errors)}`); });
    // a negative example must be rejected
    for (const [i, ex] of (schema['x-invalid-examples'] ?? []).entries()) {
      if (validate(ex)) fail(`${file}: invalid example ${i} was accepted`);
    }
    ok(`${file}: schema compiles, ${examples.length} valid and ${(schema['x-invalid-examples'] ?? []).length} invalid examples checked`);
  }
}
console.log(failed === 0 ? 'contracts: all valid' : `contracts: ${failed} failure(s)`);
process.exit(failed === 0 ? 0 : 1);
