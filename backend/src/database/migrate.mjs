import { readdir, readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { loadConfig } from '../config.mjs';
import { createPool } from './pool.mjs';

export async function migrate(pool) {
  const client = await pool.connect();
  let locked = false;
  try {
    await client.query('SELECT pg_advisory_lock(17012610)');
    locked = true;
    await client.query(
      'CREATE TABLE IF NOT EXISTS schema_migrations (name TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())'
    );
    const folder = new URL('../../migrations/', import.meta.url);
    const files = (await readdir(folder)).filter((name) => /^\d+_[a-z0-9_]+\.sql$/.test(name)).sort();
    for (const name of files) {
      const existing = await client.query('SELECT 1 FROM schema_migrations WHERE name=$1', [name]);
      if (existing.rowCount) continue;
      await client.query('BEGIN');
      try {
        await client.query(await readFile(new URL(name, folder), 'utf8'));
        await client.query('INSERT INTO schema_migrations(name) VALUES($1)', [name]);
        await client.query('COMMIT');
      } catch (error) {
        await client.query('ROLLBACK');
        throw error;
      }
    }
  } finally {
    if (locked) await client.query('SELECT pg_advisory_unlock(17012610)');
    client.release();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const pool = createPool(loadConfig().databaseUrl);
  try {
    await migrate(pool);
    console.log('Database migrations are up to date.');
  } finally {
    await pool.end();
  }
}
