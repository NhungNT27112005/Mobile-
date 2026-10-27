import test from 'node:test';
import assert from 'node:assert/strict';
import { hashPassword, verifyPassword } from '../src/auth/passwords.mjs';

test('passwords use independent salts and verify the exact original value', async () => {
  const value = ' Fixture password 123 ';
  const first = await hashPassword(value);
  const second = await hashPassword(value);
  assert.notEqual(first, second);
  assert.equal(await verifyPassword(value, first), true);
  assert.equal(await verifyPassword(value.trim(), first), false);
  assert.equal(first.includes(value), false);
});

test('malformed password hashes are rejected safely', async () => {
  assert.equal(await verifyPassword('fixture', 'broken'), false);
  assert.equal(await verifyPassword('fixture', null), false);
});
