# Project Management System

Spring Boot midterm project. Users create **projects**, add **tasks** to them, organize tasks with **tags**
and upload an **attachment** for each task.

**Tech:** Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation), H2 database.

## Running

```bash
./mvnw spring-boot:run
```

The app runs on `http://localhost:8080`.

| What               | Where                                                                       |
|--------------------|-----------------------------------------------------------------------------|
| H2 database console | `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/pms-db`, user `sa`, no password) |
| HTML upload form   | `http://localhost:8080/upload.html`                                         |
| Uploaded files     | `./uploads` folder                                                          |

## Project structure

```
com.pms
├── config        FileStorageConfig        (@Configuration – creates the upload folder)
├── controller    Project/Task/TagController (@RestController)
├── service       Project/Task/Tag/FileStorageService (@Service, @Transactional)
├── repository    Project/Task/TagRepository (@Repository, extend JpaRepository)
├── entity        Project, Task, Tag, TaskStatus (@Entity)
├── dto           request/response records with validation annotations
└── exception     custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
```

## Data model

| Entity  | Fields                                                           |
|---------|------------------------------------------------------------------|
| Project | id, name, description, createdAt                                 |
| Task    | id, title, description, status, dueDate, attachmentPath, project |
| Tag     | id, name, color                                                  |

- `Project` **@OneToMany** `Task` — deleting a project deletes its tasks
- `Task` **@ManyToOne** `Project`
- `Task` **@ManyToMany** `Tag` (join table `task_tags`)

## API

### Projects

| Method | URL                                  | Description                                   |
|--------|--------------------------------------|-----------------------------------------------|
| POST   | `/api/projects`                      | Create a project                              |
| GET    | `/api/projects?page=0&size=10&sort=name,asc` | List projects (paged, newest first by default) |
| GET    | `/api/projects/{id}`                 | Get one project                               |
| PUT    | `/api/projects/{id}`                 | Update a project                              |
| DELETE | `/api/projects/{id}`                 | Delete a project and its tasks                |
| POST   | `/api/projects/{projectId}/tasks`    | Create a task inside a project                |
| GET    | `/api/projects/{projectId}/tasks?status=TODO` | Tasks of a project (status is optional) |

### Tasks

| Method | URL                                              | Description                                 |
|--------|--------------------------------------------------|---------------------------------------------|
| GET    | `/api/tasks?page=0&size=10&sort=dueDate,desc`    | List tasks (paged and sorted)               |
| GET    | `/api/tasks/{id}`                                | Get one task                                |
| PUT    | `/api/tasks/{id}`                                | Update a task                               |
| DELETE | `/api/tasks/{id}`                                | Delete a task                               |
| GET    | `/api/tasks/search?title=login&status=TODO`      | Search by title, optionally by status (paged) |
| GET    | `/api/tasks/overdue`                             | Unfinished tasks whose due date has passed  |
| GET    | `/api/tasks/by-tag?name=backend`                 | Tasks that have the given tag               |
| PUT    | `/api/tasks/{id}/tags`                           | Assign tags, body `{ "tagIds": [1, 2] }`    |
| POST   | `/api/tasks/{id}/attachment`                     | Upload a file (`multipart/form-data`, field `file`) |

### Tags

| Method | URL         | Description                |
|--------|-------------|----------------------------|
| POST   | `/api/tags` | Create a tag               |
| GET    | `/api/tags` | List all tags (by name)    |

### Example requests

```bash
# Create a project
curl -X POST localhost:8080/api/projects -H "Content-Type: application/json" \
     -d '{"name": "Website", "description": "Company website"}'

# Create a task inside project 1
curl -X POST localhost:8080/api/projects/1/tasks -H "Content-Type: application/json" \
     -d '{"title": "Login page", "description": "Build it", "status": "TODO", "dueDate": "2026-12-01"}'

# Create a tag and assign it to task 1
curl -X POST localhost:8080/api/tags -H "Content-Type: application/json" \
     -d '{"name": "backend", "color": "#FF5733"}'
curl -X PUT localhost:8080/api/tasks/1/tags -H "Content-Type: application/json" -d '{"tagIds": [1]}'

# Upload an attachment for task 1
curl -X POST localhost:8080/api/tasks/1/attachment -F "file=@/path/to/document.pdf"
```

### Postman

`postman/Project-Management-System.postman_collection.json` has 32 requests covering every endpoint, each with
tests. With the app running, run the whole collection in order: it creates data, checks every feature, then
deletes what it created.

- **Postman app:** Import the file → open the collection → **Run**. If the upload request shows the file as
  missing, select `postman/sample-attachment.txt` in its Body tab.
- **Postman CLI** (from the project folder): `postman collection run postman/Project-Management-System.postman_collection.json`

Invalid input returns `400` with the failing fields:

```json
{ "status": 400, "detail": "Validation failed", "errors": { "name": "Project name is required" } }
```

## Assignment requirements → implementation

| Requirement                         | Where                                                                 |
|-------------------------------------|-----------------------------------------------------------------------|
| Layered packages + stereotypes      | `controller`, `service`, `repository`, `entity`, `config` packages    |
| Constructor injection only          | Every controller/service receives dependencies through its constructor |
| 3+ entities, 2 relationship types   | `Project`, `Task`, `Tag` — `@OneToMany`/`@ManyToOne` and `@ManyToMany` |
| 2+ method-name queries              | `TaskRepository`: `findByTitleContainingIgnoreCase`, `findByTitleContainingIgnoreCaseAndStatus`, `findByProjectId`, `findByProjectIdAndStatus` |
| 2+ `@Query` queries                 | `TaskRepository`: `findOverdueTasks`, `findByTagName`                 |
| `@Transactional`                    | All service methods that change data (read methods use `readOnly = true`) |
| HTTP method annotations             | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` in controllers |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | e.g. `TaskController.uploadAttachment`, `searchTasks`, `updateTask` |
| Validation + `@Valid`               | `dto` records (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@NotEmpty`) |
| File upload (`MultipartFile`)       | `TaskController.uploadAttachment` → `FileStorageService`; path saved in `Task.attachmentPath`; `upload.html` form uses `enctype="multipart/form-data"` |
| Pagination & sorting                | `Pageable` + `@PageableDefault` on list/search endpoints, `Sort` in `TagService` and `findOverdueTasks` |
