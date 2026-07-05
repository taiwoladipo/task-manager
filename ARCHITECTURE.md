# Task Manager REST API Architecture

The application is a Spring Boot task management REST API with persistence, validation,
seeded data, and optimistic concurrency control.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web + Spring Data JPA
- Maven
- MySQL
- H2 (tests)
- Docker

## Coding Challenge Solution

- [x] Validation
  - `title`, `author`, and `project` are required.
  - `(title, author)` must be unique.
  - `project` must reference an existing project id.
  - Requests with unknown payload fields are rejected.
  - Status must be one of `pending`, `in-progress`, or `completed`.
  - ETag header is required for `PUT /api/tasks/{id}` for concurrency handling.
- [x] Unit tests
  - Tests cover API behavior (request/response) and validation.
- [x] Optional core enhancements
  - Filter tasks by status (`GET /api/tasks?status=...`).
  - Normalized `Task -> Project` relation using a foreign key.
  - Optimistic locking using version on `Task` for concurrency handling. Stale updates return `409 Conflict`.

## Design Decisions

- Data is seeded during startup via a `CommandLineRunner` bean executed by Spring Boot once the application context is initialized.
  The runner inserts projects and tasks as specified in the instructions.
  The task data has been modified to include a `project` reference according to the schema normalization enhancement.
- Centralized API error handling via the `ApiExceptionHandler` class using `ProblemDetail` to propagate actual error
  messages, such as validation errors, to the response.
- Version-based optimistic update: the data version is managed using the built-in JPA `@Version` property on the entity.
  Early conflict detection and clear client feedback were implemented as follows:
  - `GET /api/tasks/{id}` returns hashed `version` as `ETag` header.
  - `PUT /api/tasks/{id}` requires an `ETag` header
  - Before database update, the `ETag` header is compared with the latest database state of the object.
    If the same, update is allowed; otherwise a `409` error is returned with message stating that the data has been modified
    by another user, to prevent unintentional override.
  - In case of successful update, `saveAndFlush` is used to ensure that the version is incremented and the new version
    is returned in the update response header. This prevents the client from having to make an additional GET request 
    to get the new ETag.
- Additional endpoints for project management were added to support the normalized schema. The following endpoints were added:
  - `GET /api/projects` - list all projects
  - `POST /api/projects` - create project
- Maven was selected due to popularity and inbuilt IDE support.

## Out of Scope (Possible Improvements)

- Authentication
- Pagination
- Sorting
- PATCH updates
- Cloud deployment
- Normalizing `Task -> Author` relation using a foreign key instead of storing the author as a string in the task table.


## Test Coverage

- **Task API**
  - Startup seeded data availability.
  - Status filtering and invalid status handling.
  - Create/update/delete happy paths.
  - Required-field validation and unknown-field rejection.
  - Invalid UUID and not-found behavior.
  - Duplicate `(title, author)` conflict checks.
  - ETag + optimistic locking:
	- update succeeds with matching ETag,
	- update fails with stale ETag,
	- update fails when ETag header is missing,
	- GET returns ETag and does not expose `version`.
- **Project API**
  - Startup default project availability.
  - Create project success.
  - Empty name validation.

## Where AI-Assisted coding was used
- The application was architected and implemented by me. Generative AI was used for time-consuming tasks
  and areas where I had knowledge gaps and a solution was not immediately obvious. The following tasks were assisted by AI:
  - Writing unit tests for the API endpoints.
  - Generating the first draft of the README.md.
  - Creating the `ApiExceptionHandler` class for centralized error handling.
  - Creating the API test bash script for end-to-end tests.
  - Dockerization of the application.