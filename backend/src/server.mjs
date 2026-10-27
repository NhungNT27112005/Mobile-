import { loadConfig } from './config.mjs';
import { createPool } from './database/pool.mjs';
import { migrate } from './database/migrate.mjs';
import { seed } from './database/seed.mjs';
import { createApp } from './app.mjs';

let pool;
try {
  const config = loadConfig();
  pool = createPool(config.databaseUrl);
  await migrate(pool);
  if (config.seed) await seed(pool);
  await pool.query('DELETE FROM sessions WHERE expires_at <= now()');
  const app = createApp({ pool, sessionTtlSeconds: config.sessionTtlSeconds, authRateLimit: config.authRateLimit });
  const server = app.listen(config.port, config.host);
  await new Promise((resolve, reject) => {
    server.once('listening', resolve);
    server.once('error', reject);
  });
  console.log('NewsHub API listening on port', config.port);

  let closing = false;
  const stop = () => {
    if (closing) return;
    closing = true;
    const timer = setTimeout(() => process.exit(1), 10000);
    timer.unref();
    server.close(async () => {
      await pool.end();
      clearTimeout(timer);
      process.exit(0);
    });
    server.closeIdleConnections();
  };
  process.on('SIGTERM', stop);
  process.on('SIGINT', stop);
} catch (error) {
  console.error('Backend startup failed:', error.code ?? error.name);
  if (pool) await pool.end();
  process.exitCode = 1;
}
