import { randomUUID } from 'node:crypto';
import express from 'express';
import helmet from 'helmet';
import { rateLimit } from 'express-rate-limit';
import { createAuthService } from './auth/service.mjs';
import { authRoutes } from './auth/routes.mjs';
import { articleRoutes } from './articles/routes.mjs';
import { categoryRoutes } from './categories/routes.mjs';
import { commentRoutes } from './comments/routes.mjs';
import { authenticate } from './middleware/authenticate.mjs';
import { errorHandler, HttpError } from './middleware/errors.mjs';

export function createApp({ pool, sessionTtlSeconds = 86400, authRateLimit = 30, logging = true }) {
  const app = express();
  app.disable('x-powered-by');
  app.use(helmet());
  app.use((req, res, next) => {
    req.requestId = randomUUID();
    res.set('X-Request-Id', req.requestId);
    res.set('Cache-Control', 'no-store');
    const start = Date.now();
    if (logging) res.on('finish', () => {
      console.log(req.method, req.path, res.statusCode, Date.now() - start + 'ms');
    });
    next();
  });

  app.get('/health', async (_req, res) => {
    try {
      await pool.query('SELECT 1');
      res.json({ status: 'ok', service: 'newshub-api' });
    } catch {
      res.status(503).json({ message: 'Cơ sở dữ liệu chưa sẵn sàng' });
    }
  });

  app.use(rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 300,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    message: { message: 'Quá nhiều yêu cầu. Vui lòng thử lại sau.' },
  }));
  app.use(express.json({ limit: '16kb' }));

  const authService = createAuthService(pool, sessionTtlSeconds);
  app.use('/auth', authRoutes(authService, authRateLimit));
  app.use('/categories', authenticate(authService), categoryRoutes(pool));
  app.use('/articles/:articleId/comments', authenticate(authService), commentRoutes(pool));
  app.use('/articles', authenticate(authService), articleRoutes(pool));

  app.use((_req, _res, next) => next(new HttpError(404, 'API không tồn tại')));
  app.use(errorHandler);
  return app;
}
