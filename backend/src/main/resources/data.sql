-- Sample data. Run by Spring Boot on every startup (spring.sql.init.mode=always), after schema.sql.
--
-- Inserts only when the table is empty, so it is safe to repeat:
--   * a new database gets the sample rows;
--   * existing data is never touched, and deleting individual URLs is permanent;
--   * if every URL is deleted, the sample rows come back on the next restart.
-- 12 rows = 3 pages at the default page size of 5.
INSERT INTO shortened_urls (alias, full_url, created_at)
SELECT alias, full_url, created_at FROM (
              SELECT 'spring-boot'    AS alias, 'https://spring.io/projects/spring-boot'                      AS full_url, '2026-01-05T09:00:00.000000000Z' AS created_at
    UNION ALL SELECT 'react-docs',     'https://react.dev/learn',                                              '2026-01-12T10:30:00.000000000Z'
    UNION ALL SELECT 'mdn-fetch',      'https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API',           '2026-01-20T14:15:00.000000000Z'
    UNION ALL SELECT 'sqlite',         'https://www.sqlite.org/docs.html',                                     '2026-02-02T08:45:00.000000000Z'
    UNION ALL SELECT 'docker-compose', 'https://docs.docker.com/compose/',                                     '2026-02-14T16:20:00.000000000Z'
    UNION ALL SELECT 'vite',           'https://vite.dev/guide/',                                              '2026-03-01T11:00:00.000000000Z'
    UNION ALL SELECT 'nginx-proxy',    'https://nginx.org/en/docs/http/ngx_http_proxy_module.html',            '2026-03-18T13:40:00.000000000Z'
    UNION ALL SELECT 'typescript',     'https://www.typescriptlang.org/docs/handbook/intro.html',              '2026-04-07T09:25:00.000000000Z'
    UNION ALL SELECT 'junit5',         'https://junit.org/junit5/docs/current/user-guide/',                    '2026-05-11T15:05:00.000000000Z'
    UNION ALL SELECT 'rfc-3986',       'https://www.rfc-editor.org/rfc/rfc3986',                               '2026-06-23T10:10:00.000000000Z'
    UNION ALL SELECT 'example',        'https://example.com/',                                                 '2026-08-04T12:00:00.000000000Z'
    UNION ALL SELECT 'openjdk',        'https://openjdk.org/',                                                 '2026-09-30T17:35:00.000000000Z'
) AS sample
WHERE NOT EXISTS (SELECT 1 FROM shortened_urls);
