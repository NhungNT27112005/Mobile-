import { ZodError } from 'zod';

export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export function errorHandler(error, req, res, next) {
  if (res.headersSent) return next(error);
  let status = 500;
  let message = 'Máy chủ gặp lỗi. Vui lòng thử lại.';
  if (error instanceof HttpError) {
    status = error.status;
    message = error.message;
  } else if (error instanceof ZodError) {
    status = 400;
    message = error.issues[0]?.message ?? 'Dữ liệu không hợp lệ';
  } else if (error.type === 'entity.parse.failed') {
    status = 400;
    message = 'JSON không hợp lệ';
  } else if (error.type === 'entity.too.large') {
    status = 413;
    message = 'Nội dung yêu cầu quá lớn';
  }
  if (status === 500) console.error('Request failed:', req.requestId, error.code ?? 'INTERNAL');
  res.status(status).json({ message });
}
