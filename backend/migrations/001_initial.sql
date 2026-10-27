CREATE TABLE users (
  id UUID PRIMARY KEY,
  display_name VARCHAR(80) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sessions (
  token_hash CHAR(64) PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX sessions_user_idx ON sessions(user_id);
CREATE INDEX sessions_expiry_idx ON sessions(expires_at);

CREATE TABLE categories (
  id VARCHAR(60) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  description TEXT,
  display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE articles (
  id VARCHAR(80) PRIMARY KEY,
  title VARCHAR(240) NOT NULL,
  summary TEXT NOT NULL,
  content TEXT NOT NULL,
  category_id VARCHAR(60) NOT NULL REFERENCES categories(id),
  source_name VARCHAR(120) NOT NULL,
  published_at TIMESTAMPTZ NOT NULL,
  image_url TEXT
);
CREATE INDEX articles_published_idx ON articles(published_at DESC, id);
CREATE INDEX articles_category_idx ON articles(category_id, published_at DESC, id);

CREATE TABLE comments (
  id UUID PRIMARY KEY,
  article_id VARCHAR(80) NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 2000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX comments_article_idx ON comments(article_id, created_at, id);
