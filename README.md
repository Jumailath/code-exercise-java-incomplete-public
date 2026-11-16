# Snip – URL Shortener

A full-stack URL shortener built with **Java Spring Boot** (backend) and **React + TypeScript** (frontend), containerized with Docker.

- Shorten a long URL, with an optional custom alias.
- Short links redirect to the original URL.
- Browse every shortened URL, newest first, a page at a time.
- Delete a short URL.

---

## Architecture

```
frontend/      React + TypeScript (Vite)
backend/       Java Spring Boot Web API + SQLite
docker-compose.yml       Orchestrates both services
```

The frontend proxies API calls through Nginx in production, so there are no CORS concerns once containerized. In development, Vite's dev server proxies to the local API.

---

## API Reference

The API exposes the same contract expected by the existing frontend.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/shorten` | Create a shortened URL |
| `GET` | `/urls` | List all shortened URLs |
| `GET` | `/{alias}` | Redirect to the full URL |
| `DELETE` | `/{alias}` | Delete a shortened URL |

### POST /shorten

```json
// Request
{
  "fullUrl": "https://example.com/very/long/path",
  "customAlias": "my-alias"   // optional
}

// Response 201
{
  "alias": "my-alias",
  "fullUrl": "https://example.com/very/long/path",
  "shortUrl": "http://localhost:8080/my-alias"
}
```

Returns `400` if the URL is invalid, the alias is malformed, or the alias is already taken.

### GET /urls
| Parameter | Default | Notes |
|---|---|---|
| `page` | `0` | 0-based page index |
| `size` | `5` | Items per page |

Invalid values, such as `size=0`, `page=-1` or non-numbers, fall back to the defaults instead of failing. This is Spring Data's standard `Pageable` behaviour.

```json
// GET /urls?page=0&size=2  →  200
{
  "urls": [
    { "alias": "openjdk", "fullUrl": "https://openjdk.org/",  "shortUrl": "http://localhost:3000/openjdk" },
    { "alias": "example", "fullUrl": "https://example.com/", "shortUrl": "http://localhost:3000/example" }
  ],
  "totalElements": 12,
  "totalPages": 6,
  "number": 0,
  "size": 2
}
```

Results are sorted by creation time, **newest first** (`ORDER BY created_at DESC, id DESC`). The `id` tiebreaker keeps the order stable for rows created at the same moment, so page boundaries don't shift between requests.

### GET /{alias}

Returns `302 Found` with a `Location` header, or `404` if the alias doesn't exist.

### DELETE /{alias}

Removes the short URL. `204` on success, `404` if the alias doesn't exist.

---

## Design Decisions & Assumptions

**Persistence** - SQLite via JDBC. The database file path is configurable via the `DATABASE_PATH` environment variable and is volume-mounted in Docker so data survives container restarts.

 **Sample data** - Spring Boot's SQL initialization (`spring.sql.init.mode=always`) runs two scripts on every startup:
- [schema.sql](backend/src/main/resources/schema.sql) creates the table with `CREATE TABLE IF NOT EXISTS`.
- [data.sql](backend/src/main/resources/data.sql) inserts 12 sample URLs (3 pages at the default page size) **only when the table is empty**.

So a new database gets sample data, existing data is never changed, and deleting individual URLs is permanent. The trade-off: if *every* URL is deleted, the sample rows come back on the next restart.


**Alias validation** - Aliases must be 2-64 characters, containing only letters, numbers, and hyphens. This mirrors typical URL shortener conventions and avoids characters that are awkward in URLs.

**Random alias generation** - 

**Error handling** - Invalid input and domain errors are translated into JSON error responses like `{ "error": "..." }`.

**Testing** - The backend includes service-level tests and can be extended with integration tests. The frontend includes React component tests and service-level API tests.

**Frontend** - The hosted frontend uses Nginx to proxy `/shorten`, `/urls`, and alias requests to the API, so the same Docker setup works without CORS issues.

## Running locally (without Docker)

Requires JDK 17+, Maven 3.9+ and Node.js 20+.

**Backend**, on http://localhost:8080:

```bash
cd backend
mvn spring-boot:run
```

The database file is created at `./urlshortener.db`, relative to the directory you start from. Set `DATABASE_PATH` to use a different file.

**Frontend**, on http://localhost:3000, with hot reload:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/shorten`, `/urls` and `/{alias}` to `localhost:8080` ([vite.config.ts](frontend/vite.config.ts)), the same routes nginx proxies in Docker.

### Configuration

| Variable | Default | Used for |
|---|---|---|
| `DATABASE_PATH` | `./urlshortener.db` | SQLite database file |
| `SERVER_PORT` | `8080` | API port |


## Run with Docker

Requires Docker Desktop to run locally

```bash
docker compose up --build
```

Then open **http://localhost:3000**.

On first start, the database is created and filled with 12 sample URLs.

| Command | What it does |
|---|---|
| `docker compose up --build` | Build the images and start everything (Ctrl+C stops it) |
| `docker compose up -d --build` | Same, in the background |
| `docker compose logs -f api` | Follow the Spring Boot logs |
| `docker compose down` | Stop and remove the containers. **The data is kept** |
| `docker compose down -v` | Also delete the data volume. The next start begins with the sample data again |

### Services run in Docker

| Service | Image built from | Port | Notes |
|---|---|---|---|
| `web` | `frontend/Dockerfile` | 3000 → 80 | nginx serves the built React app and proxies `/shorten`, `/urls` and `/{alias}` to `api` ([nginx.conf](frontend/nginx.conf)). The browser only talks to one origin, so CORS doesn't apply. |
| `api` | `backend/Dockerfile` | 8080 | Spring Boot API, running as a non-root user. Port 8080 is published for calling the API directly. |
| `url-data` (volume) | — | — | Holds the SQLite file `/data/urlshortener.db`, so data survives restarts and rebuilds. |

---

## Known limitations

- **Security** - At present there is no authentication or rate limiting. Anyone who can reach the API can create and delete URLs. Also screen for malicious URLs.
- **Database** - A shared deployment with several API instances would need a server database such as PostgreSQL. 
- **Timestamp precision.** - created_at` is stored as `Instant.toString()` text, which prints a varying number of fractional-second digits. Two URLs created within the *same second* can therefore sort slightly out of order. A fixed-width format, or storing epoch milliseconds, would fix this.
- **API quality** - Version API and document it with swagger. Design API to use /v1/urls as root with POST, GET. GET alias and DELETE alias to use /v1/urls/{alias} to maintain standards.
- **Observability** - Spring boot actuator to check service health. Add structured logs with a request.
- **Performance** - Add caching for alias lookups.
- **Deployment** - CI/CD pipeline using tools like GitHub actions to build, run tests, scan, build image and deploy. Also add configuration per environment.