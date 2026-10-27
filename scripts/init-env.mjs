import { randomBytes } from 'node:crypto';
import { writeFile } from 'node:fs/promises';

const password = randomBytes(32).toString('base64url');
const text = [
  'POSTGRES_USER=newshub', 'POSTGRES_DB=newshub',
  'POSTGRES_PASSWORD=' + password,
  'DB_PORT=5433', 'API_PORT=8000',
  'DATABASE_URL=postgresql://newshub:' + password + '@127.0.0.1:5433/newshub',
  'SESSION_TTL_SECONDS=86400', 'AUTH_RATE_LIMIT=30', 'SEED_DEMO_DATA=true', '',
].join('\n');
try {
  await writeFile(new URL('../.env', import.meta.url), text, { flag: 'wx', mode: 0o600 });
  console.log('Created private .env with a random database password.');
} catch (error) {
  if (error.code !== 'EEXIST') throw error;
  console.log('.env already exists; no settings were overwritten.');
}
