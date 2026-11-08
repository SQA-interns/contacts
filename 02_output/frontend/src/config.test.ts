import { describe, expect, it } from 'vitest';
import { readConfig } from './config';

describe('runtime configuration', () => {
  it('prefers the container values over the build values', () => {
    const config = readConfig(
      { VITE_API_BASE_URL: 'https://api.example.test/' },
      { VITE_API_BASE_URL: 'http://localhost:8080' },
    );
    expect(config.apiBaseUrl).toBe('https://api.example.test');
  });

  it('falls back to the build values and to localhost for the API', () => {
    expect(readConfig({}, { VITE_FIREBASE_PROJECT_ID: 'demo' }).firebase.projectId).toBe('demo');
    expect(readConfig({}, {}).apiBaseUrl).toBe('http://localhost:8080');
  });

  it('treats an empty emulator address as real Firebase', () => {
    expect(readConfig({ VITE_FIREBASE_AUTH_EMULATOR_URL: '' }, {}).authEmulatorUrl).toBe('');
  });
});
