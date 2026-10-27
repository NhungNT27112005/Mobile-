import { HttpError } from '../middleware/errors.mjs';

const columns = 'id,title,summary,category_id AS "categoryId",source_name AS "sourceName",published_at AS "publishedAt",image_url AS "imageUrl"';

export function createArticleRepository(pool) {
  return {
    async list({ q, categoryId, limit, offset }) {
      const result = await pool.query(
        'SELECT ' + columns + ' FROM articles WHERE ($1 = \'\' OR strpos(lower(title || \' \' || summary),lower($1)) > 0) AND ($2 = \'\' OR category_id=$2) ORDER BY published_at DESC,id LIMIT $3 OFFSET $4',
        [q, categoryId, limit, offset]
      );
      return result.rows;
    },
    async detail(id) {
      const result = await pool.query('SELECT ' + columns + ',content FROM articles WHERE id=$1', [id]);
      if (!result.rowCount) throw new HttpError(404, 'Không tìm thấy bài viết');
      return result.rows[0];
    },
  };
}
