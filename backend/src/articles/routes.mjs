import { Router } from 'express';
import { z } from 'zod';
import { createArticleRepository } from './repository.mjs';

const querySchema = z.object({
  q: z.string().trim().max(200).default(''),
  categoryId: z.string().trim().max(60).default(''),
  limit: z.coerce.number().int().min(1).max(100).default(50),
  offset: z.coerce.number().int().min(0).max(1000000).default(0),
}).strict();

export function articleRoutes(pool) {
  const router = Router();
  const repository = createArticleRepository(pool);
  router.get('/', async (req, res) => res.json(await repository.list(querySchema.parse(req.query))));
  router.get('/:id', async (req, res) => res.json(await repository.detail(req.params.id)));
  return router;
}
