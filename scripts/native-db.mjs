import { randomBytes } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';

const require = createRequire(new URL('../backend/package.json', import.meta.url));
const { Pool } = require('pg');
const dotenv = require('dotenv');
const local = new URL('../backend/.local/', import.meta.url);
const mode = process.argv[2];
await mkdir(local, { recursive: true });

if (mode === 'configure') {
  const databasePort = Number(process.argv[3] ?? 55432);
  const apiPort = Number(process.argv[4] ?? 8000);
  for (const port of [databasePort, apiPort]) {
    if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error('Invalid local port');
  }
  let password;
  try {
    password = (await readFile(new URL('postgres-password.txt', local), 'utf8')).trim();
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    password = randomBytes(32).toString('base64url');
    await writeFile(new URL('postgres-password.txt', local), password, { flag: 'wx', mode: 0o600 });
  }
  const text = [
    'DATABASE_URL=postgresql://newshub:' + password + '@127.0.0.1:' + databasePort + '/newshub_dev',
    'TEST_DATABASE_URL=postgresql://newshub:' + password + '@127.0.0.1:' + databasePort + '/newshub_test',
    'HOST=0.0.0.0', 'PORT=' + apiPort,
    'SESSION_TTL_SECONDS=86400', 'AUTH_RATE_LIMIT=30', 'SEED_DEMO_DATA=true', '',
  ].join('\n');
  try {
    const existing = dotenv.parse(await readFile(new URL('native.env', local), 'utf8'));
    if (new URL(existing.DATABASE_URL).port !== String(databasePort) || existing.PORT !== String(apiPort)) {
      throw new Error('Local ports differ from existing settings; keep the previous ports.');
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await writeFile(new URL('native.env', local), text, { flag: 'wx', mode: 0o600 });
  }
  console.log('Private local backend configuration is ready.');
} else if (mode === 'create-databases') {
  const settings = dotenv.parse(await readFile(new URL('native.env', local), 'utf8'));
  const url = new URL(settings.DATABASE_URL);
  url.pathname = '/postgres';
  const pool = new Pool({ connectionString: url.href, connectionTimeoutMillis: 5000 });
  try {
    for (const name of ['newshub_dev', 'newshub_test']) {
      const result = await pool.query('SELECT 1 FROM pg_database WHERE datname=$1', [name]);
      if (!result.rowCount) await pool.query('CREATE DATABASE ' + name);
    }
    console.log('Dedicated development/test databases are ready.');
  } finally {
    await pool.end();
  }
} else {
  throw new Error('Use configure or create-databases');
}
