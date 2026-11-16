-- Run by Spring Boot on every startup (spring.sql.init.mode=always), before data.sql.
-- IF NOT EXISTS keeps it safe to repeat on an existing database.
CREATE TABLE IF NOT EXISTS shortened_urls (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    alias      TEXT NOT NULL UNIQUE,
    full_url   TEXT NOT NULL,
    created_at TEXT NOT NULL
);
