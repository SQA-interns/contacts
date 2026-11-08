/** Runtime configuration: `window.__CONFIG__` (written by the container at start) first, then Vite variables. */
export interface RuntimeConfig {
  apiBaseUrl: string;
  firebase: { apiKey: string; authDomain: string; projectId: string };
  /** Empty means real Firebase. */
  authEmulatorUrl: string;
}

type RawConfig = Partial<Record<string, string>>;

declare global {
  interface Window {
    __CONFIG__?: RawConfig;
  }
}

export function readConfig(
  runtime: RawConfig = typeof window === 'undefined' ? {} : (window.__CONFIG__ ?? {}),
  env: RawConfig = import.meta.env as RawConfig,
): RuntimeConfig {
  const pick = (name: string, fallback = ''): string => {
    const fromRuntime = runtime[name];
    if (fromRuntime !== undefined && fromRuntime !== '') return fromRuntime;
    return env[name] ?? fallback;
  };
  return {
    apiBaseUrl: pick('VITE_API_BASE_URL', 'http://localhost:8080').replace(/\/+$/, ''),
    firebase: {
      apiKey: pick('VITE_FIREBASE_API_KEY'),
      authDomain: pick('VITE_FIREBASE_AUTH_DOMAIN'),
      projectId: pick('VITE_FIREBASE_PROJECT_ID'),
    },
    authEmulatorUrl: pick('VITE_FIREBASE_AUTH_EMULATOR_URL'),
  };
}
