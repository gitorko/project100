# Project 100

Stock exchange order matching demo with configurable whole-order or partial-fill matching. The backend uses Java 21, Spring Boot 4, Spring Data JPA, and PostgreSQL. The Angular UI is bundled into the executable jar for production builds.

Project overview: [Stock Exchange – Price Time Priority Algorithm](https://gitorko.github.io/stock-exchange/)

## Run with Docker Compose

Requirements: Docker with Compose v2, Java 21, Node.js, and Yarn.

Build the application and UI, then start PostgreSQL and the application:

```bash
./gradlew cleanBuild
docker compose -f docker/docker-compose.yml up --build
```

Open [http://localhost:8080](http://localhost:8080). The API is available under `/api`. PostgreSQL data is stored in a named Docker volume and survives container restarts. Stop the services with `Ctrl+C`; use `docker compose -f docker/docker-compose.yml down` to remove the containers. To remove the database data too, add `-v` to the `down` command.

The Compose file uses PostgreSQL 18.7 and local demo credentials. Set `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_DB` in the Compose environment before exposing this demo beyond a local machine.

## Local development

Start PostgreSQL 18.7:

```bash
docker run --name project100-db \
  -e POSTGRES_USER=stock_exchange \
  -e POSTGRES_PASSWORD=stock_exchange \
  -e POSTGRES_DB=stock_exchange \
  -p 5432:5432 \
  -d postgres:18.7
```

Run the Spring Boot app in another terminal:

```bash
./gradlew bootRun
```

Run the Angular development server separately:

```bash
cd ui
yarn install
yarn start
```

Open [http://localhost:4200](http://localhost:4200). The UI dev server proxies API calls to the backend.

To override the database connection, set `SPRING_DATASOURCE_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` in the environment.

## API requests

The Bruno collection is in [`bruno/`](bruno/). Open that folder in Bruno and select the **Local** environment.

Main endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/order` | Submit a buy or sell order |
| `GET` | `/api/open-order` | List today's open orders |
| `GET` | `/api/settled-order` | List today's settled orders |
| `DELETE` | `/api/order` | Clear demo data |
| `GET` | `/api/simulate-random/{records}` | Generate mixed orders |
| `GET` | `/api/simulate-buy/{records}` | Generate buy orders |
| `GET` | `/api/simulate-sell/{records}` | Generate sell orders |

Example order body:

```json
{
  "ticker": "GOOGL",
  "price": 10.0,
  "type": "BUY",
  "quantity": 100
}
```

## Build and tests

Gradle is configured in `gradle.properties` to prefer IPv4 when connecting to
plugin and Maven repositories, so no extra network options are needed on the
command line.

```bash
./gradlew clean build
```

Integration tests use Testcontainers and need a working Docker installation. To build the UI and package it into the executable jar, run:

```bash
./gradlew cleanBuild
```

On macOS with Rancher Desktop, point Testcontainers at the active Docker context and use the socket path inside the Rancher VM:

```bash
DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
./gradlew test
```

The packaged jar is written to `build/libs/`. Run it with `java -jar build/libs/project100-1.0.0.jar`.

To enable partial fills with Docker Compose, set the matching mode before starting the app:

```bash
STOCK_EXCHANGE_MATCHING_MODE=PARTIAL docker compose -f docker/docker-compose.yml up --build
```

The default is `WHOLE_ORDER`, which preserves the original behavior. The equivalent Spring property is `stock-exchange.matching.mode`; accepted values are `WHOLE_ORDER` and `PARTIAL`.

## Matching model

Orders are queued and processed by a single worker per ticker. The in-memory order book tracks price buckets and FIFO order chains. In `WHOLE_ORDER` mode, the engine searches for a combination of complete resting orders whose quantities exactly match the incoming order. In `PARTIAL` mode, it matches by best price then arrival order, creates fill summaries, and persists any unfilled remainder as an open order. Open orders are stored in PostgreSQL so the application can recover them after a restart.
