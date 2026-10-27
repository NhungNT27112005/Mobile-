import { createHash, randomBytes, randomUUID } from 'node:crypto';
import { DUMMY_HASH, hashPassword, verifyPassword } from './passwords.mjs';
import { HttpError } from '../middleware/errors.mjs';

export const hashToken = (token) => createHash('sha256').update(token).digest('hex');
const userDto = (row) => ({ id: row.id, displayName: row.display_name, email: row.email });

export function createAuthService(pool, sessionTtlSeconds) {
  return {
    async register({ displayName, email, password }) {
      const passwordHash = await hashPassword(password);
      try {
        await pool.query(
          'INSERT INTO users(id,display_name,email,password_hash) VALUES($1,$2,$3,$4)',
          [randomUUID(), displayName, email, passwordHash]
        );
      } catch (error) {
        if (error.code === '23505') throw new HttpError(409, 'Email đã được sử dụng');
        throw error;
      }
    },

    async login({ email, password }) {
      const result = await pool.query('SELECT * FROM users WHERE email=$1', [email]);
      const row = result.rows[0];
      const valid = await verifyPassword(password, row?.password_hash ?? DUMMY_HASH);
      if (!row || !valid) throw new HttpError(401, 'Email hoặc mật khẩu không đúng');
      const accessToken = randomBytes(32).toString('base64url');
      const expiresAt = Math.floor(Date.now() / 1000) + sessionTtlSeconds;
      await pool.query(
        'INSERT INTO sessions(token_hash,user_id,expires_at) VALUES($1,$2,to_timestamp($3))',
        [hashToken(accessToken), row.id, expiresAt]
      );
      return { accessToken, expiresAt, user: userDto(row) };
    },

    async authenticate(token) {
      const result = await pool.query(
        'SELECT u.id,u.display_name,u.email FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=$1 AND s.expires_at > now()',
        [hashToken(token)]
      );
      if (!result.rowCount) throw new HttpError(401, 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn');
      return { user: userDto(result.rows[0]), tokenHash: hashToken(token) };
    },

    async logout(tokenHash) {
      await pool.query('DELETE FROM sessions WHERE token_hash=$1', [tokenHash]);
    },
  };
}
