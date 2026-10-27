import { randomUUID } from 'node:crypto';
import { Router } from 'express';
import { z } from 'zod';
import { HttpError } from '../middleware/errors.mjs';

const bodySchema = z.object({
  content: z.string().trim().min(1, 'Vui lòng nhập bình luận').max(2000, 'Bình luận tối đa 2000 ký tự'),
}).strict();

export function commentRoutes(pool) {
  const router = Router({ mergeParams: true });

  router.get('/', async (req, res) => {
    const article = await pool.query('SELECT 1 FROM articles WHERE id=$1', [req.params.articleId]);
    if (!article.rowCount) throw new HttpError(404, 'Không tìm thấy bài viết');
    const result = await pool.query(
      'SELECT c.id,c.article_id AS "articleId",c.user_id AS "userId",u.display_name AS "displayName",c.content,c.created_at AS "createdAt" FROM comments c JOIN users u ON u.id=c.user_id WHERE c.article_id=$1 ORDER BY c.created_at,c.id',
      [req.params.articleId]
    );
    res.json(result.rows);
  });

  router.post('/', async (req, res) => {
    const { content } = bodySchema.parse(req.body);
    const result = await pool.query(
      'INSERT INTO comments(id,article_id,user_id,content) SELECT $1,a.id,$3,$4 FROM articles a WHERE a.id=$2 RETURNING id,article_id AS "articleId",user_id AS "userId",content,created_at AS "createdAt"',
      [randomUUID(), req.params.articleId, req.auth.user.id, content]
    );
    if (!result.rowCount) throw new HttpError(404, 'Không tìm thấy bài viết');
    res.status(201).json({ ...result.rows[0], displayName: req.auth.user.displayName });
  });
  return router;
}
