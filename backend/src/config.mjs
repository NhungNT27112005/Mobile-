import dotenv from 'dotenv';

dotenv.config({ path: new URL('../../.env', import.meta.url), quiet: true });

function integer(name, fallback, min, max) {
  const value = Number(process.env[name] ?? fallback);
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new Error('Invalid configuration: ' + name);
  }
  return value;
}

export function loadConfig() {
  let databaseUrl;
  try {
    databaseUrl = new URL(process.env.DATABASE_URL ?? '');
  } catch {
    throw new Error('DATABASE_URL is required; see backend/README.md');
  }
  if (!['postgres:', 'postgresql:'].includes(databaseUrl.protocol)) {
    throw new Error('DATABASE_URL must use PostgreSQL');
  }
  const seed = process.env.SEED_DEMO_DATA ?? 'true';
  if (!['true', 'false'].includes(seed)) throw new Error('Invalid SEED_DEMO_DATA');

  return {
    databaseUrl: databaseUrl.href,
    host: process.env.HOST ?? '0.0.0.0',
    port: integer('PORT', 8000, 1, 65535),
    sessionTtlSeconds: integer('SESSION_TTL_SECONDS', 86400, 60, 2592000),
    authRateLimit: integer('AUTH_RATE_LIMIT', 30, 1, 10000),
    seed: seed === 'true',
  };
}
