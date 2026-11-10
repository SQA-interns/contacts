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

  it('uses the build value when the container value is empty, and keeps a non-empty container value', () => {
    expect(
      readConfig({ VITE_FIREBASE_API_KEY: '' }, { VITE_FIREBASE_API_KEY: 'from-build' }).firebase.apiKey,
    ).toBe('from-build');
    expect(
      readConfig({ VITE_FIREBASE_API_KEY: 'from-container' }, { VITE_FIREBASE_API_KEY: 'from-build' })
        .firebase.apiKey,
    ).toBe('from-container');
    expect(readConfig({}, {}).firebase).toEqual({ apiKey: '', authDomain: '', projectId: '' });
  });

  it('removes every trailing slash of the API address', () => {
    expect(readConfig({ VITE_API_BASE_URL: 'https://a.test///' }, {}).apiBaseUrl).toBe('https://a.test');
  });

  it('reads window.__CONFIG__ when no runtime object is given', () => {
    window.__CONFIG__ = { VITE_FIREBASE_AUTH_DOMAIN: 'auth.example.test' };
    try {
      expect(readConfig(undefined, {}).firebase.authDomain).toBe('auth.example.test');
    } finally {
      delete window.__CONFIG__;
    }
    expect(readConfig(undefined, {}).firebase.authDomain).toBe('');
  });
});
