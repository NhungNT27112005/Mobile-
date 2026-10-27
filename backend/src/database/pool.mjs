import pg from 'pg';

export function createPool(connectionString, options = {}) {
  const pool = new pg.Pool({
    connectionString,
    max: 8,
    connectionTimeoutMillis: 5000,
    statement_timeout: 10000,
    ...options,
  });
  pool.on('error', (error) => {
    console.error('Database connection error:', error.code ?? 'UNKNOWN');
  });
  return pool;
}
