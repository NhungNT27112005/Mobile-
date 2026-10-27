import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { loadConfig } from '../config.mjs';
import { createPool } from './pool.mjs';
import { migrate } from './migrate.mjs';

export async function seed(pool) {
  const data = JSON.parse(await readFile(new URL('../../seeds/news.json', import.meta.url), 'utf8'));
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    for (const [index, category] of data.categories.entries()) {
      await client.query(
        'INSERT INTO categories(id,name,description,display_order) VALUES($1,$2,$3,$4) ON CONFLICT(id) DO NOTHING',
        [category.id, category.name, category.description, index]
      );
    }
    for (const article of data.articles) {
      await client.query(
        'INSERT INTO articles(id,title,summary,content,category_id,source_name,published_at,image_url) VALUES($1,$2,$3,$4,$5,$6,$7,$8) ON CONFLICT(id) DO NOTHING',
        [article.id, article.title, article.summary, article.content, article.categoryId, article.sourceName, article.publishedAt, article.imageUrl]
      );
    }
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const pool = createPool(loadConfig().databaseUrl);
  try {
    await migrate(pool);
    await seed(pool);
    console.log('Sample categories/articles inserted; existing data preserved.');
  } finally {
    await pool.end();
  }
}
