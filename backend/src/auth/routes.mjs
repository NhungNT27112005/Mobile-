import { Router } from 'express';
import { rateLimit } from 'express-rate-limit';
import { z } from 'zod';
import { authenticate } from '../middleware/authenticate.mjs';

const email = z.string().trim().max(254).email('Email không hợp lệ').transform((value) => value.toLowerCase());
const password = z.string().min(8, 'Mật khẩu cần ít nhất 8 ký tự').max(128, 'Mật khẩu quá dài')
  .refine((value) => value.trim().length > 0, 'Vui lòng nhập mật khẩu');
const loginSchema = z.object({ email, password }).strict();
const registerSchema = z.object({
  displayName: z.string().trim().min(2, 'Tên hiển thị cần ít nhất 2 ký tự').max(80),
  email,
  password,
}).strict();

export function authRoutes(authService, authRateLimit) {
  const router = Router();
  const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: authRateLimit,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    message: { message: 'Bạn đã thử quá nhiều lần. Vui lòng thử lại sau.' },
  });

  router.post('/register', authLimiter, async (req, res) => {
    await authService.register(registerSchema.parse(req.body));
    res.status(201).json({ message: 'Đăng ký thành công. Vui lòng đăng nhập.' });
  });

  router.post('/login', authLimiter, async (req, res) => {
    res.json(await authService.login(loginSchema.parse(req.body)));
  });

  router.get('/me', authenticate(authService), (req, res) => res.json(req.auth.user));

  router.post('/logout', authenticate(authService), async (req, res) => {
    await authService.logout(req.auth.tokenHash);
    res.status(204).end();
  });

  return router;
}
