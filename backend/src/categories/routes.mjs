import { Router } from 'express';

export function categoryRoutes(pool) {
  const router = Router();
  router.get('/', async (_req, res) => {
    const result = await pool.query('SELECT id,name,description FROM categories ORDER BY display_order,id');
    res.json(result.rows);
  });
  return router;
}
