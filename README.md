# Task Manager REST API

Spring Boot REST API for managing tasks with MySQL persistence.

## Prerequisites

- For local run and local tests (without Docker):
  - Java 21+
  - MySQL 8+ (for app runtime)
  - Maven Wrapper (`./mvnw`, included in repo)
- For Docker run:
  - Docker engine-compatible runtime (Docker Desktop / Colima / Podman machine)
- Optional tools:
  - `curl` for test script `scripts/test-api.sh`

## API Endpoints

### Task Endpoints

#### `GET /api/tasks`

Optional query parameter: `status` (`pending`, `in-progress`, `completed`).

Example request:

```bash
curl -s http://localhost:8080/api/tasks
```

Example response:

```json
{
  "tasks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Implement User Authentication",
      "author": "Alice Johnson",
      "description": "Create a secure user authentication system using JWT.",
      "project": {
        "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
        "name": "Tasks API",
        "created_at": "2025-09-28T08:00:00Z"
      },
      "status": "pending",
      "created_at": "2025-09-29T13:23:16Z",
      "updated_at": "2025-09-29T13:23:16Z"
    }
  ]
}
```

#### `GET /api/tasks/{id}`

Returns one task and an `ETag` header for conditional update.

Example request:

```bash
curl -i -s http://localhost:8080/api/tasks/550e8400-e29b-41d4-a716-446655440000
```

Example response headers (excerpt):

```text
HTTP/1.1 200
ETag: 5feceb66ffc86f38d952786c6d696c79...
```

#### `POST /api/tasks`

Example request:

```bash
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Write API docs",
    "author": "Carol",
    "project": { "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11" },
    "description": "Document all task endpoints.",
    "status": "pending"
  }'
```

Example response:

```json
{
  "id": "...",
  "title": "Write API docs",
  "author": "Carol",
  "description": "Document all task endpoints.",
  "project": { "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", "name": null, "created_at": null },
  "status": "pending",
  "created_at": "...",
  "updated_at": "..."
}
```

#### `PUT /api/tasks/{id}`

Requires `ETag` from a previous `GET /api/tasks/{id}`.

Example request:

```bash
curl -s -X PUT http://localhost:8080/api/tasks/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -H "ETag: 5feceb66ffc86f38d952786c6d696c79..." \
  -d '{
    "title": "Implement User Authentication",
    "author": "Alice Johnson",
    "project": { "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11" },
    "description": "Updated description",
    "status": "in-progress"
  }'
```

Example response headers (excerpt):

```text
HTTP/1.1 200
ETag: 6b86b273ff34fce19d6b804eff5a3f57...
```

If `ETag` is missing -> `400`. If stale -> `409`.

#### `DELETE /api/tasks/{id}`

Example request:

```bash
curl -s -X DELETE http://localhost:8080/api/tasks/550e8400-e29b-41d4-a716-446655440000
```

Example response:

```json
{
  "success": "true"
}
```

### Project Endpoints

#### `GET /api/projects`

Example request:

```bash
curl -s http://localhost:8080/api/projects
```

Example response:

```json
{
  "projects": [
    {
      "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
      "name": "Tasks API",
      "created_at": "2025-09-28T08:00:00Z"
    }
  ]
}
```

#### `POST /api/projects`

Example request:

```bash
curl -s -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Billing"}'
```

Example response:

```json
{
  "id": "...",
  "name": "Billing",
  "created_at": "..."
}
```

Validation notes:

- `project` is required in task create/update payloads.
- `description` is optional.
- If task `status` is omitted, it defaults to `pending`.
- Allowed status values: `pending`, `in-progress`, `completed`.

## Running the App Locally

The application can be run either directly on the machine (see prerequisites) or in a container environment.

### Option A: Run directly on the machine (requires Java + MySQL)
1. Ensure MySQL is running and credentials in `src/main/resources/application.properties` are valid.
2. Start the app:

```bash
./mvnw spring-boot:run
```

3. Verify API is reachable:

```bash
curl -s http://localhost:8080/api/tasks
```

### Option B: Run with Docker

1. Start app + MySQL with Docker Compose:

```bash
docker compose up --build -d
```

2. Verify API is reachable:

```bash
curl -s http://localhost:8080/api/tasks
```

3. Stop containers when done:

```bash
docker compose down
```

## Testing

Tests run on in-memory H2 database with this command:

```bash
./mvnw test
```

## API Test Script

The repository also includes `scripts/test-api.sh` to run a full end-to-end endpoint test sequence against a running app.

To run with default base URL (`http://localhost:8080`):

```bash
./scripts/test-api.sh
```

To run against a different URL:

```bash
BASE_URL=http://localhost:8080 ./scripts/test-api.sh
```

