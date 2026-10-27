import { HttpError } from './errors.mjs';

export function authenticate(authService) {
  return async (req, _res, next) => {
    const match = /^Bearer ([A-Za-z0-9_-]{43})$/.exec(req.headers.authorization ?? '');
    if (!match) throw new HttpError(401, 'Vui lòng đăng nhập');
    req.auth = await authService.authenticate(match[1]);
    next();
  };
}
