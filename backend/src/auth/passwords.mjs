import { randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';

const derive = promisify(scrypt);
const options = { N: 65536, r: 8, p: 2, maxmem: 128 * 1024 * 1024 };
export const DUMMY_HASH = 'scrypt-v1$' + '00'.repeat(16) + '$' + '00'.repeat(64);

export async function hashPassword(password) {
  const salt = randomBytes(16);
  const key = await derive(password, salt, 64, options);
  return 'scrypt-v1$' + salt.toString('hex') + '$' + key.toString('hex');
}

export async function verifyPassword(password, encoded) {
  if (!/^scrypt-v1\$[a-f0-9]{32}\$[a-f0-9]{128}$/.test(encoded ?? '')) return false;
  const [, salt, expected] = encoded.split('$');
  const key = await derive(password, Buffer.from(salt, 'hex'), 64, options);
  return timingSafeEqual(key, Buffer.from(expected, 'hex'));
}
