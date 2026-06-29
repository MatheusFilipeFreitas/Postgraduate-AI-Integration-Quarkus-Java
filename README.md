# Postgraduate AI Integration — Quarkus & Java

Travel agency backend built with [Quarkus](https://quarkus.io/) and [LangChain4j](https://docs.langchain4j.dev/) for AI-powered features via [Ollama](https://ollama.com/). Booking operations (lookup and cancellation) are delegated to a separate [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) over the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/).

Input and output [guardrails](docs/guardrails.md) validate customer messages and assistant responses for scope and professional tone.

This repository is part of a postgraduate course on integrating AI into Java applications using modern cloud-native tooling.

## Related project

| Project | Role | Default port |
|---------|------|--------------|
| [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) | MCP server — booking tools | `8081` |
| **travel-agency** (this repo) | AI travel agent — RAG + Ollama + MCP client + guardrails | `8080` |

```shell
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java.git booking-mcp-server
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-AI-Integration-Quarkus-Java.git travel-agency
```

**Start booking-mcp-server before travel-agency** so MCP tools are available when the agent handles booking questions.

## Tech stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Quarkus | 3.36.3 |
| LangChain4j (Quarkus extension) | 3.36.3 |
| AI provider | Ollama (`quarkus-langchain4j-ollama`) |
| RAG | pgvector (`quarkus-langchain4j-pgvector`) — see [docs/pgvector.md](docs/pgvector.md) |
| Booking tools | MCP client (`quarkus-langchain4j-mcp`) — see [docs/mcp.md](docs/mcp.md) |
| Guardrails | Input + output guardrails — see [docs/guardrails.md](docs/guardrails.md) |
| Database | PostgreSQL 16 + pgvector (`docker-compose.yml`) |
| Build tool | Maven |

## Prerequisites

- JDK 25+
- [Ollama](https://ollama.com/download) installed and running locally
- The `gemma3:4b` chat model pulled in Ollama: `ollama pull gemma3:4b`
- The `nomic-embed-text` embedding model pulled in Ollama (required for RAG): `ollama pull nomic-embed-text`
- Docker (for PostgreSQL + pgvector): `docker compose up -d`

## Getting started

### 1. Clone the repositories

```shell
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-AI-Integration-Quarkus-Java.git
cd Postgraduate-AI-Integration-Quarkus-Java
```

Also clone [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) alongside this project if you have not already.

### 2. Start PostgreSQL

```shell
docker compose up -d
```

### 3. Start the booking MCP server

In the `booking-mcp-server` directory:

```shell
./mvnw quarkus:dev
```

MCP endpoint: `http://localhost:8081/mcp/sse`

### 4. Run travel-agency in dev mode

```shell
./mvnw quarkus:dev
```

Quarkus Dev UI: [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/)

### 5. Ollama, pgvector, and MCP configuration

Ollama and pgvector settings are in `src/main/resources/application.properties`. You need a running Ollama instance, PostgreSQL started via Docker Compose, the chat and embedding models pulled, and documents under `src/main/resources/rag/`.

MCP client settings (booking server):

```properties
quarkus.langchain4j.mcp.booking-server.transport-type=http
quarkus.langchain4j.mcp.booking-server.url=http://localhost:8081/mcp/sse
```

Guardrail retry limit:

```properties
quarkus.langchain4j.guardrails.max-retries=3
```

For the full RAG property list, ingestion pipeline, and production guidance, see **[docs/pgvector.md](docs/pgvector.md)**. For MCP booking tools, see **[docs/mcp.md](docs/mcp.md)**. For guardrails (prompt security, tone, headers), see **[docs/guardrails.md](docs/guardrails.md)**. To compare RAG approaches, see **[docs/easy-rag-vs-pgvector.md](docs/easy-rag-vs-pgvector.md)**.

### 6. Travel agent API

With both services running, send a plain-text question to the travel agent. Optionally pass the authenticated user via header (defaults to `Guest`):

```shell
# RAG — travel packages
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -H "X-User-Name: Jane Smith" \
  -d "What travel packages do you offer?"

# MCP — booking lookup (requires booking-mcp-server on port 8081)
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -H "X-User-Name: Jane Smith" \
  -d "What are the details of booking 12345?"
```

The agent uses pgvector-backed RAG for package information and MCP tools from booking-mcp-server for reservations. Out-of-scope or malicious prompts are blocked by the input guardrail — see [docs/guardrails.md](docs/guardrails.md) for examples.

See the [booking-mcp-server README](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java/blob/main/README.md) for mock booking ids and cancellation rules.

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

```text
.
├── docker-compose.yml               # PostgreSQL 16 + pgvector
├── docs/
│   ├── easy-rag.md                  # Easy RAG configuration (earlier setup)
│   ├── easy-rag-vs-pgvector.md      # Comparison of both RAG approaches
│   ├── guardrails.md                # Input/output guardrails guide
│   ├── mcp.md                       # MCP client and booking tools guide
│   └── pgvector.md                  # pgvector configuration and usage guide
├── pom.xml
├── src/main/java/com/mathffreitas/travel/
│   ├── ai/
│   │   ├── TravelAgentAssistent.java      # Main LangChain4j AI service
│   │   ├── PromptSecurityAssistent.java   # Prompt injection classifier
│   │   ├── ToneJudge.java                 # Response tone classifier
│   │   ├── config/ChatMemoryConfig.java
│   │   ├── guard/
│   │   │   ├── InjectionGuard.java        # Input guardrail
│   │   │   ├── ToneGuardrail.java         # Output guardrail
│   │   │   └── JsonStructureGuard.java    # JSON output guardrail (preview)
│   │   └── instructions/AssistentInstructions.java
│   ├── rag/
│   │   ├── DocumentIngest.java
│   │   └── RagConfiguration.java
│   └── resource/
│       └── TravelAgentResource.java       # REST endpoint (POST /travel)
├── src/main/docker/
└── src/main/resources/
    ├── application.properties
    └── rag/
        └── plans-travel.md
```

## Related documentation

- [Guardrails (this project)](docs/guardrails.md)
- [MCP booking tools (this project)](docs/mcp.md)
- [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) — booking MCP server setup and tools
- [pgvector configuration (this project)](docs/pgvector.md)
- [Easy RAG vs pgvector (this project)](docs/easy-rag-vs-pgvector.md)
- [Easy RAG configuration (earlier setup)](docs/easy-rag.md)
- [Quarkus guides](https://quarkus.io/guides/)
- [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus LangChain4j — MCP integration](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [LangChain4j guardrails](https://docs.langchain4j.dev/tutorials/guardrails/)
- [Quarkus LangChain4j — PGVector Document Store](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)

## Author

Matheus Filipe Freitas
