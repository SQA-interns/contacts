import { ESLint } from 'eslint';
import { describe, expect, it } from 'vitest';

const eslint = new ESLint({ cwd: process.cwd() });

async function messages(code: string, filePath: string): Promise<string[]> {
  const [result] = await eslint.lintText(code, { filePath });
  return (result?.messages ?? []).map((m) => `${m.ruleId}: ${m.message}`);
}

describe('architecture lint rules (AR-03, AR-08)', () => {
  it('AR-08: literal UI text in JSX is refused', async () => {
    const out = await messages('export const A = () => <p>Hello</p>;\n', 'src/features/x/A.tsx');
    expect(out.some((m) => m.startsWith('no-restricted-syntax'))).toBe(true);
  });

  it('AR-08: literal accessible names are refused', async () => {
    const out = await messages(
      'export const A = () => <button aria-label="Close" />;\n',
      'src/features/x/A.tsx',
    );
    expect(out.some((m) => m.startsWith('no-restricted-syntax'))).toBe(true);
  });

  it('AR-08: text from the catalog is accepted', async () => {
    const out = await messages(
      'export const A = ({ t }: { t: (k: string) => string }) => <p>{t("a")}</p>;\n',
      'src/features/x/A.tsx',
    );
    expect(out.filter((m) => m.startsWith('no-restricted-syntax'))).toEqual([]);
  });

  it('AR-03: Firebase may be imported only in src/auth', async () => {
    const code = "import { getAuth } from 'firebase/auth';\nexport const a = getAuth;\n";
    expect(
      (await messages(code, 'src/features/x/a.ts')).some((m) => m.startsWith('no-restricted-imports')),
    ).toBe(true);
    expect((await messages(code, 'src/auth/a.ts')).some((m) => m.startsWith('no-restricted-imports'))).toBe(
      false,
    );
  });

  it('AR-03: fetch may be called only in src/api', async () => {
    const code = "export const load = () => fetch('/x');\n";
    expect(
      (await messages(code, 'src/features/x/a.ts')).some((m) => m.startsWith('no-restricted-globals')),
    ).toBe(true);
    expect((await messages(code, 'src/api/a.ts')).some((m) => m.startsWith('no-restricted-globals'))).toBe(
      false,
    );
  });

  it('R-018-14, SR-05: HTML built from text is refused', async () => {
    const code =
      'export const A = ({ text }: { text: string }) => <p dangerouslySetInnerHTML={{ __html: text }} />;\n';
    const out = await messages(code, 'src/features/x/A.tsx');
    expect(out.some((m) => m.startsWith('no-restricted-syntax') && m.includes('plain text'))).toBe(true);
  });
});
