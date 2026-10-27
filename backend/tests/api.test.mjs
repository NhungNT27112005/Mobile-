import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { createServer } from 'node:http';
import { createPool } from '../src/database/pool.mjs';
import { migrate } from '../src/database/migrate.mjs';
import { seed } from '../src/database/seed.mjs';
import { createApp } from '../src/app.mjs';
import { hashToken } from '../src/auth/service.mjs';

const databaseUrl = process.env.TEST_DATABASE_URL;
if (!databaseUrl || !new URL(databaseUrl).pathname.toLowerCase().includes('test')) {
  throw new Error('Set TEST_DATABASE_URL to a dedicated test database; no production fallback is allowed.');
}
const schema = 'test_' + randomUUID().replaceAll('-', '');
if (!/^test_[a-f0-9]{32}$/.test(schema)) throw new Error('Invalid isolated test schema');
const adminPool = createPool(databaseUrl);
let pool;
const servers = [];
let base;
let fixture;
let commentId;
const password = 'Test-only-pass!123';

async function start(options = {}) {
  const server = createServer(createApp({ pool, authRateLimit: 100, logging: false, ...options }));
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  servers.push(server);
  return 'http://127.0.0.1:' + server.address().port;
}
async function request(method, path, body, token, root = base) {
  const response = await fetch(root + path, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null, headers: response.headers };
}
async function account() {
  const email = 'fixture-' + randomUUID() + '@example.com';
  const registration = await request('POST', '/auth/register', { displayName: 'Fixture User', email, password });
  assert.equal(registration.status, 201);
  const login = await request('POST', '/auth/login', { email, password });
  assert.equal(login.status, 200);
  return { ...login.body, email };
}

before(async () => {
  await adminPool.query('CREATE SCHEMA "' + schema + '"');
  pool = createPool(databaseUrl, { options: '-c search_path=' + schema });
  await migrate(pool);
  await seed(pool);
  base = await start();
  fixture = await account();
});

after(async () => {
  for (const server of servers) {
    server.closeIdleConnections();
    await new Promise((resolve) => server.close(resolve));
  }
  if (pool) await pool.end();
  await adminPool.query('DROP SCHEMA IF EXISTS "' + schema + '" CASCADE');
  await adminPool.end();
});

test('health is public; application data and comment writes require authentication', async () => {
  assert.equal((await request('GET', '/health')).body.status, 'ok');
  for (const path of ['/categories', '/articles', '/auth/me', '/articles/ai-learning/comments']) {
    assert.equal((await request('GET', path)).status, 401);
  }
  assert.equal((await request('POST', '/articles/ai-learning/comments', { content: 'fixture' })).status, 401);
  assert.equal((await request('POST', '/auth/logout')).status, 401);
});

test('registration validates fields and normalizes email; it does not create a session', async () => {
  const email = 'case-' + randomUUID() + '@example.com';
  const result = await request('POST', '/auth/register', {
    displayName: '  Normalized User  ', email: ' ' + email.toUpperCase() + ' ', password,
  });
  assert.equal(result.status, 201);
  assert.equal('accessToken' in result.body, false);
  assert.equal((await request('POST', '/auth/register', { displayName: 'Other', email, password })).status, 409);
  const login = await request('POST', '/auth/login', { email: email.toUpperCase(), password });
  assert.equal(login.status, 200);
  assert.equal(login.body.user.email, email);
  assert.equal(login.body.user.displayName, 'Normalized User');
  for (const bad of [
    { displayName: 'A', email, password },
    { displayName: 'Fixture', email: 'bad', password },
    { displayName: 'Fixture', email, password: 'short' },
    { displayName: 'Fixture', email, password: '        ' },
    { displayName: 'Fixture', email, password, role: 'admin' },
  ]) assert.equal((await request('POST', '/auth/register', bad)).status, 400);
});

test('login returns Android-compatible user/token fields and rejects wrong credentials', async () => {
  assert.equal(typeof fixture.accessToken, 'string');
  assert.equal(fixture.accessToken.length, 43);
  assert.ok(fixture.expiresAt > Date.now() / 1000);
  assert.deepEqual(Object.keys(fixture.user).sort(), ['displayName', 'email', 'id']);
  assert.equal((await request('POST', '/auth/login', { email: fixture.email, password: 'incorrect123' })).status, 401);
  assert.equal((await request('POST', '/auth/login', { email: 'missing@example.com', password })).status, 401);
  assert.equal((await request('POST', '/auth/login', { email: 'invalid', password })).status, 400);
});

test('only password hashes and token digests are stored', async () => {
  const user = await pool.query('SELECT password_hash FROM users WHERE id=$1', [fixture.user.id]);
  assert.equal(user.rows[0].password_hash.startsWith('scrypt-v1$'), true);
  assert.equal(user.rows[0].password_hash.includes(password), false);
  const sessions = await pool.query('SELECT token_hash FROM sessions WHERE user_id=$1', [fixture.user.id]);
  assert.equal(sessions.rows.some((row) => row.token_hash === hashToken(fixture.accessToken)), true);
  assert.equal(sessions.rows.some((row) => row.token_hash === fixture.accessToken), false);
});

test('profile and session remain available when the HTTP application is recreated', async () => {
  const restarted = await start();
  const result = await request('GET', '/auth/me', undefined, fixture.accessToken, restarted);
  assert.equal(result.status, 200);
  assert.deepEqual(result.body, fixture.user);
});

test('categories return the Category model fields', async () => {
  const result = await request('GET', '/categories', undefined, fixture.accessToken);
  assert.equal(result.status, 200);
  assert.equal(result.body.length, 3);
  assert.deepEqual(Object.keys(result.body[0]).sort(), ['description', 'id', 'name']);
});

test('article summaries match the Android model and omit full content', async () => {
  const result = await request('GET', '/articles', undefined, fixture.accessToken);
  assert.equal(result.status, 200);
  assert.equal(result.body.length, 6);
  assert.equal('content' in result.body[0], false);
  assert.deepEqual(Object.keys(result.body[0]).sort(),
    ['categoryId', 'id', 'imageUrl', 'publishedAt', 'sourceName', 'summary', 'title']);
  assert.ok(Number.isFinite(Date.parse(result.body[0].publishedAt)));
});

test('search and category filters combine with AND; SQL-looking input stays literal', async () => {
  const tech = await request('GET', '/articles?q=AI&categoryId=technology', undefined, fixture.accessToken);
  assert.equal(tech.body.length, 1);
  assert.equal(tech.body[0].id, 'ai-learning');
  assert.equal((await request('GET', '/articles?q=AI&categoryId=education', undefined, fixture.accessToken)).body.length, 0);
  assert.equal((await request('GET', '/articles?categoryId=missing', undefined, fixture.accessToken)).body.length, 0);
  const injection = encodeURIComponent("' OR 1=1 --");
  assert.equal((await request('GET', '/articles?q=' + injection, undefined, fixture.accessToken)).body.length, 0);
});

test('pagination is bounded and invalid query values are rejected', async () => {
  const first = await request('GET', '/articles?limit=2', undefined, fixture.accessToken);
  const second = await request('GET', '/articles?limit=2&offset=2', undefined, fixture.accessToken);
  assert.equal(first.body.length, 2);
  assert.equal(second.body.length, 2);
  assert.notEqual(first.body[0].id, second.body[0].id);
  for (const query of ['limit=0', 'limit=101', 'offset=-1', 'q=' + 'a'.repeat(201), 'unknown=x']) {
    assert.equal((await request('GET', '/articles?' + query, undefined, fixture.accessToken)).status, 400);
  }
});

test('detail includes full content and missing articles return JSON 404', async () => {
  const result = await request('GET', '/articles/ai-learning', undefined, fixture.accessToken);
  assert.equal(result.status, 200);
  assert.ok(result.body.content.length > result.body.summary.length);
  assert.equal((await request('GET', '/articles/not-found', undefined, fixture.accessToken)).status, 404);
});

test('comments use the authenticated author and persist with camelCase model fields', async () => {
  const initial = await request('GET', '/articles/ai-learning/comments', undefined, fixture.accessToken);
  assert.equal(initial.body.length, 0);
  const created = await request('POST', '/articles/ai-learning/comments', { content: '  Bình luận kiểm thử  ' }, fixture.accessToken);
  assert.equal(created.status, 201);
  assert.equal(created.body.userId, fixture.user.id);
  assert.equal(created.body.displayName, fixture.user.displayName);
  assert.equal(created.body.content, 'Bình luận kiểm thử');
  assert.ok(Number.isFinite(Date.parse(created.body.createdAt)));
  commentId = created.body.id;
  const list = await request('GET', '/articles/ai-learning/comments', undefined, fixture.accessToken);
  assert.equal(list.body.some((comment) => comment.id === commentId), true);
});

test('comment validation rejects blank/oversized content and userId spoofing', async () => {
  for (const body of [{ content: ' ' }, { content: 'x'.repeat(2001) }, { content: 'fixture', userId: randomUUID() }]) {
    assert.equal((await request('POST', '/articles/ai-learning/comments', body, fixture.accessToken)).status, 400);
  }
  assert.equal((await request('POST', '/articles/missing/comments', { content: 'fixture' }, fixture.accessToken)).status, 404);
  assert.equal((await request('GET', '/articles/missing/comments', undefined, fixture.accessToken)).status, 404);
});

test('logout invalidates only the current token', async () => {
  const user = await account();
  const another = await request('POST', '/auth/login', { email: user.email, password });
  assert.equal((await request('POST', '/auth/logout', undefined, user.accessToken)).status, 204);
  assert.equal((await request('GET', '/auth/me', undefined, user.accessToken)).status, 401);
  assert.equal((await request('GET', '/auth/me', undefined, another.body.accessToken)).status, 200);
});

test('expired and malformed tokens are rejected', async () => {
  const user = await account();
  await pool.query("UPDATE sessions SET expires_at=now()-interval '1 second' WHERE token_hash=$1", [hashToken(user.accessToken)]);
  assert.equal((await request('GET', '/auth/me', undefined, user.accessToken)).status, 401);
  assert.equal((await request('GET', '/auth/me', undefined, 'malformed')).status, 401);
});

test('migrations and seed are repeatable without erasing user comments', async () => {
  await migrate(pool);
  await seed(pool);
  const comments = await pool.query('SELECT 1 FROM comments WHERE id=$1', [commentId]);
  assert.equal(comments.rowCount, 1);
  assert.equal((await pool.query('SELECT count(*)::int AS count FROM articles')).rows[0].count, 6);
  assert.equal((await pool.query('SELECT count(*)::int AS count FROM schema_migrations')).rows[0].count, 1);
});

test('invalid JSON, oversized requests, unknown routes and rate limits use safe JSON errors', async () => {
  const malformed = await fetch(base + '/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{',
  });
  assert.equal(malformed.status, 400);
  assert.equal((await request('POST', '/auth/login', { oversized: 'x'.repeat(17000) })).status, 413);
  const missing = await request('GET', '/not-a-route');
  assert.equal(missing.status, 404);
  assert.deepEqual(Object.keys(missing.body), ['message']);
  assert.equal(missing.headers.get('cache-control'), 'no-store');
  const limited = await start({ authRateLimit: 1 });
  assert.equal((await request('POST', '/auth/login', { email: 'bad', password }, undefined, limited)).status, 400);
  assert.equal((await request('POST', '/auth/login', { email: 'bad', password }, undefined, limited)).status, 429);
});
