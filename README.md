# Postgraduate AI Integration — Quarkus & Java

Travel agency backend built with [Quarkus](https://quarkus.io/) and [LangChain4j](https://docs.langchain4j.dev/) for AI-powered features via [Ollama](https://ollama.com/).

This repository is part of a postgraduate course on integrating AI into Java applications using modern cloud-native tooling.

## Tech stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Quarkus | 3.36.3 |
| LangChain4j (Quarkus extension) | 3.36.3 |
| AI provider | Ollama (`quarkus-langchain4j-ollama`) |
| RAG | pgvector (`quarkus-langchain4j-pgvector`) — see [docs/pgvector.md](docs/pgvector.md) |
| Database | PostgreSQL 16 + pgvector (`docker-compose.yml`) |
| Build tool | Maven |

## Prerequisites

- JDK 25+
- [Ollama](https://ollama.com/download) installed and running locally
- The `gemma3:4b` chat model pulled in Ollama: `ollama pull gemma3:4b`
- The `nomic-embed-text` embedding model pulled in Ollama (required for RAG): `ollama pull nomic-embed-text`
- Docker (for PostgreSQL + pgvector): `docker compose up -d`

## Getting started

### 1. Clone the repository

```shell
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-AI-Integration-Quarkus-Java.git
cd Postgraduate-AI-Integration-Quarkus-Java
```

### 2. Start PostgreSQL

```shell
docker compose up -d
```

### 3. Run in dev mode

```shell
./mvnw quarkus:dev
```

Quarkus Dev UI is available at [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/) while running in dev mode.

### 4. Ollama and pgvector configuration

Ollama and pgvector settings are in `src/main/resources/application.properties`. You need a running Ollama instance, PostgreSQL started via Docker Compose, the chat and embedding models pulled, and documents under `src/main/resources/rag/`.

For the full property list, ingestion pipeline, and production guidance, see **[docs/pgvector.md](docs/pgvector.md)**. To compare this setup with the earlier Easy RAG approach, see **[docs/easy-rag-vs-pgvector.md](docs/easy-rag-vs-pgvector.md)**.

### 5. Travel agent API

With the app running in dev mode, send a plain-text question to the travel agent:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "What travel packages do you offer?"
```

The agent uses pgvector-backed RAG to answer from documents in `src/main/resources/rag/` (for example, `plans-travel.md` with package details). See [docs/pgvector.md](docs/pgvector.md) for how ingestion and retrieval are configured.

## Build and run

Package the application:

```shell
./mvnw package
```

Run the packaged JAR:

```shell
java -jar target/quarkus-app/quarkus-run.jar
```

Build an über-jar:

```shell
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

## Native executable

With GraalVM installed:

```shell
./mvnw package -Dnative
```

Or build inside a container:

```shell
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Run the native binary:

```shell
./target/travel-agency-1.0.0-runner
```

## Docker

Dockerfiles are provided under `src/main/docker/` for JVM, legacy JAR, native, and native-micro builds.

## Project structure

```
.
├── docker-compose.yml               # PostgreSQL 16 + pgvector
├── docs/
│   ├── easy-rag.md                  # Easy RAG configuration (earlier setup)
│   ├── easy-rag-vs-pgvector.md      # Comparison of both RAG approaches
│   └── pgvector.md                  # pgvector configuration and usage guide
├── pom.xml
├── src/main/java/com/mathffreitas/travel/
│   ├── ai/
│   │   └── TravelAgentAssistent.java    # LangChain4j AI service interface
│   ├── rag/
│   │   ├── DocumentIngest.java        # Loads and embeds documents into PostgreSQL
│   │   └── RagConfiguration.java      # Wires the retrieval augmentor
│   └── resource/
│       └── TravelAgentResource.java   # REST endpoint (POST /travel)
├── src/main/docker/                 # Docker build files
└── src/main/resources/
    ├── application.properties       # Ollama / LangChain4j configuration
    └── rag/
        └── plans-travel.md          # Travel package knowledge base for RAG
```

## Related documentation

- [pgvector configuration (this project)](docs/pgvector.md)
- [Easy RAG vs pgvector (this project)](docs/easy-rag-vs-pgvector.md)
- [Easy RAG configuration (earlier setup)](docs/easy-rag.md)
- [Quarkus guides](https://quarkus.io/guides/)
- [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus LangChain4j — PGVector Document Store](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)

## Author

Matheus Filipe Freitas
