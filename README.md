# algocode

A LeetCode-style practice sandbox for Java with an IntelliJ-flavoured editor, a sandboxed
judge that measures **runtime and memory**, a free-form **scratchpad**, and progress
mechanics (XP, levels, streaks, achievements, leaderboard) to keep practice engaging.

| | |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Flyway, H2 (dev) / PostgreSQL |
| Frontend | React 19, TypeScript, Vite, Monaco editor |
| Judge | javac in-process + a harness JVM per run, either a local child process or a locked-down Docker container |
| Editor intelligence | javac-backed diagnostics, completion (with auto-import), hover and parameter info |
| Telemetry | one `AlgoTelemetry` interface, implemented with Micrometer (OpenTelemetry-ready) |

## Quick start (development)

Requirements: JDK 21, Maven 3.9, Node 22.

```bash
# terminal 1: backend on :8080 (H2 file database in backend/data, LOCAL sandbox)
cd backend && mvn spring-boot:run

# terminal 2: frontend on :5173, proxies /api to :8080
cd frontend && npm install && npm run dev
```

Open http://localhost:5173. There are no accounts yet: the browser picks a handle
(changeable on the Progress page) and sends it in the `X-Algocode-User` header.

> `LOCAL` sandbox mode runs submissions in a child JVM on your machine with only a heap cap
> and a kill timer. It is for development. Use `DOCKER` mode anywhere other people can submit code.

## Run with Docker (local)

```bash
docker compose up --build        # app on http://localhost:3000, API on http://localhost:8080
```

This starts PostgreSQL, the backend and the frontend. The backend runs in `DOCKER` sandbox
mode: it uses the host's Docker daemon (through `/var/run/docker.sock`) to start one
throwaway, network-less container per run, and pulls the sandbox image on start-up.

Both images work on their own too:

| Image | Port | Settings |
|---|---|---|
| `backend/Dockerfile` | 8080 | `ALGOCODE_DB_URL` / `_USER` / `_PASSWORD`, `ALGOCODE_SANDBOX_MODE`, `DOCKER_HOST`; health at `/actuator/health/{liveness,readiness}` |
| `frontend/Dockerfile` | 8080 | `BACKEND_URL` (default `http://backend:8080`); nginx serves the app and proxies `/api`; health at `/healthz` |

Both run as non-root users. Every push to `main` publishes multi-arch images to
`ghcr.io/anzab-dev/algocode-backend` and `ghcr.io/anzab-dev/algocode-frontend`, tagged
`latest` and `sha-<commit>` (`.github/workflows/images.yml`).

## Deploy to Kubernetes

```bash
kubectl apply -k deploy/k8s
kubectl -n algocode port-forward svc/frontend 8080:80    # or use the Ingress (host algocode.local)
```

`deploy/k8s` (Kustomize) creates the `algocode` namespace with:

* **backend**: Deployment with startup, liveness and readiness probes, plus a **Docker-in-Docker
  sidecar** that runs the sandbox containers. The backend reaches it on the pod's loopback
  (`DOCKER_HOST=tcp://127.0.0.1:2375`). The sidecar has to be privileged; on clusters that forbid
  that, schedule the backend on a node pool that allows it or use a rootless or Sysbox runtime.
* **frontend**: two nginx replicas behind a Service, and an Ingress routing to them.
* **postgres**: a single-instance StatefulSet with a 2 Gi volume. Change the password in
  `postgres.yaml`, or point `ALGOCODE_DB_URL` at a managed database and drop the file.

Before the first deploy: GHCR packages start out private, so either make both packages public
in GitHub or add an `imagePullSecret`. To deploy a specific build, set `newTag: sha-<commit>` in
`deploy/k8s/kustomization.yaml`. CI validates the rendered manifests with kubeconform.

## How judging works

```
 browser ──POST /api/problems/{slug}/submit──▶ JudgeService
                                               │
                     JavaSourceCompiler (javac, in memory, no annotation processing)
                                               │ class files + algocode.harness.* + job.json
                                               ▼
                         SandboxExecutor ── LOCAL: child JVM
                                         └─ DOCKER: docker run --network none --read-only
                                                    --cap-drop ALL --pids-limit --memory …
                                               │ one nonce-prefixed JSON line on stdout
                                               ▼
                     OutputComparator (EXACT / UNORDERED / UNORDERED_DEEP / FLOAT) ─▶ verdict
```

* The **harness** (`backend/src/main/java/algocode/harness`) depends on `java.base` only. It
  converts JSON arguments to the method's declared types (arrays, `List<List<Integer>>`,
  `char[][]`, maps, …), runs each test on a thread with a 256 MB stack, captures `System.out`,
  and reports the return value (or the mutated first argument for `void` in-place methods).
* **Runtime** is the sum of `System.nanoTime()` around each call, so JVM start-up is excluded.
  **Memory** is peak heap above the pre-run baseline (SerialGC, small young generation, so it
  tracks retained data) plus total bytes allocated by the solution thread.
* Verdicts: Accepted, Wrong Answer (with the first failing case), Runtime Error (trace trimmed to
  user frames), Time Limit Exceeded, Memory Limit Exceeded, Compile Error (with positions).
* Accepted runs are ranked against earlier accepted submissions ("beats 83.4%"), with runtime and
  memory histograms.
* In Docker mode, class files travel into the container as a tar stream on stdin and are
  unpacked into a tmpfs, so no host path is shared. `DockerSandboxIntegrationTest` checks that
  the network is unreachable, the root file system is read-only and runaway code is killed.

## Editor: IntelliJ-style assistance

The backend runs javac's parser and attribution (`JavacTask.analyze()`) on every request, so
the editor knows real types, not just tokens.

* **On-the-fly diagnostics**: compiler errors and lint warnings, plus inspections javac lacks
  (unused local variables, unused imports, drawn faded like IntelliJ).
* **Completion**: members of the receiver's actual type with generics substituted
  (`Map<String, List<Integer>>.put(String k, List<Integer> v)`), statics after a class name,
  locals/fields/methods in scope, camel-hump matching (`gOD` → `getOrDefault`) and JDK types
  with an **auto-import** edit (`HashM` → `HashMap` + `import java.util.HashMap;`).
* **Parameter info**, **quick documentation on hover**, and **live templates**: `sout`, `soutv`,
  `souf`, `psvm`, `fori`, `forr`, `iter`, `ifn`, `inn`, `thr`, …
* IntelliJ keymap favourites: `Ctrl+D` duplicate line, `Ctrl+Y` delete line, `Ctrl+W` extend
  selection, `Ctrl+P` parameter info, `F2`/`Shift+F2` next/previous problem, `Alt+Enter`, `Shift+F6`.
* Problem page: `Ctrl+'` Run, `Ctrl+Enter` Submit. Scratchpad: `Ctrl+Enter` Run, `Ctrl+S` Save.

## Scratchpad

Any class with `public static void main(String[] args)`, with custom stdin, output capture and
time/memory figures. Saved scratchpads live on the Scratchpad page; every problem also has its
own scratchpad tab next to the solution editor for trying ideas without touching your answer.

## Adding a problem

Each problem is a folder under `backend/src/main/resources/problems/<slug>/`:

| File | Content |
|---|---|
| `problem.yaml` | title, difficulty, tags, method, params, return type, compare mode, time limit, hints, starter code |
| `description.md` | the statement (Markdown) |
| `Solution.java` | reference solution; also computes expected answers for users' custom inputs |
| `tests.json` | `[{"args": [...], "expected": ..., "sample": true}]` — samples are shown and used by Run |

On start-up `ProblemSeeder` upserts problems whose files changed (matched by slug, by content hash).
`scripts/generate_problem_tests.py` regenerates the large randomized tests, and
`ProblemCatalogIntegrityTest` runs every reference solution against every test in CI.

## Telemetry

Application code reports domain events through `dev.algocode.telemetry.AlgoTelemetry`
(`executionFinished`, `submissionJudged`, `languageRequest`, and `observe(...)` for spans).
The default `MicrometerTelemetry` turns them into Micrometer meters and observations, visible at
`/actuator/metrics` (e.g. `algocode.submissions`, `algocode.execution.wall`,
`algocode.submission.runtime`, `algocode.language`).

To ship them to an OpenTelemetry collector later, add `spring-boot-starter-opentelemetry` to
`backend/pom.xml` and set `management.otlp.metrics.export.url` /
`management.opentelemetry.tracing.export.otlp.endpoint`; observations then become spans without
code changes. To send events somewhere else entirely, provide another `AlgoTelemetry` bean.

## API

| Method | Path | |
|---|---|---|
| GET | `/api/problems`, `/api/problems/daily`, `/api/problems/{slug}` | catalog |
| POST | `/api/problems/{slug}/run` | samples + custom inputs, not recorded |
| POST | `/api/problems/{slug}/submit` | full judge, recorded, returns percentiles, XP and new achievements |
| GET | `/api/problems/{slug}/submissions`, `/api/problems/{slug}/stats`, `/api/submissions/{id}` | history and distributions |
| GET/POST/PUT/DELETE | `/api/scratchpads[/{id}]`, POST `/api/scratchpads/run` | scratchpads |
| POST | `/api/lang/diagnostics`, `/completion`, `/hover`, `/signature` | editor intelligence |
| GET | `/api/me`, `/api/leaderboard` | progress |

## Configuration

| Property / env | Default | |
|---|---|---|
| `ALGOCODE_SANDBOX_MODE` | `LOCAL` | `LOCAL` or `DOCKER` |
| `ALGOCODE_SANDBOX_IMAGE` | `eclipse-temurin:21-jre-alpine` | image for DOCKER mode |
| `algocode.sandbox.max-heap-mb` | 256 | `-Xmx` of the judged JVM |
| `algocode.sandbox.max-concurrent` | 4 | parallel runs; others queue |
| `ALGOCODE_DB_URL` / `_USER` / `_PASSWORD` | H2 file | JDBC settings |
| `ALGOCODE_CORS_ORIGINS` | `http://localhost:5173` | extra browser origins; not needed behind the frontend's proxy |

## Tests

```bash
cd backend && mvn verify      # harness, judge, language service, API, catalog integrity, Docker sandbox (if Docker is available)
cd frontend && npm test && npm run build
```
