import { randomBytes, randomUUID } from 'node:crypto';

const root = (process.env.NEWSHUB_API_URL ?? 'http://localhost:8000').replace(/\/$/, '');
const email = 'demo-' + randomUUID() + '@example.com';
const password = randomBytes(24).toString('base64url');

async function call(method, path, body, token) {
  const response = await fetch(root + path, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.text();
  if (!response.ok) throw new Error(method + ' ' + path + ' failed: ' + response.status);
  return data ? JSON.parse(data) : null;
}

await call('GET', '/health');
await call('POST', '/auth/register', { displayName: 'API Demo', email, password });
const login = await call('POST', '/auth/login', { email, password });
try {
  const user = await call('GET', '/auth/me', undefined, login.accessToken);
  const categories = await call('GET', '/categories', undefined, login.accessToken);
  const articles = await call('GET', '/articles', undefined, login.accessToken);
  console.log('HTTP API demo passed:', JSON.stringify({
    profile: Boolean(user.id), categories: categories.length, articles: articles.length,
  }));
  console.log('A temporary demo account was created; credentials and token are not printed.');
} finally {
  await call('POST', '/auth/logout', undefined, login.accessToken);
}
