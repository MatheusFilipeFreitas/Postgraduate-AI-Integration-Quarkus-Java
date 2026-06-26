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
| RAG | Easy RAG (`quarkus-langchain4j-easy-rag`) — see [docs/easy-rag.md](docs/easy-rag.md) |
| Build tool | Maven |

## Prerequisites

- JDK 25+
- [Ollama](https://ollama.com/download) installed and running locally
- The `gemma3:4b` chat model pulled in Ollama: `ollama pull gemma3:4b`
- The `nomic-embed-text` embedding model pulled in Ollama (required for Easy RAG): `ollama pull nomic-embed-text`

## Getting started

### 1. Clone the repository

```shell
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-AI-Integration-Quarkus-Java.git
cd Postgraduate-AI-Integration-Quarkus-Java
```

### 2. Run in dev mode

```shell
./mvnw quarkus:dev
```

Quarkus Dev UI is available at [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/) while running in dev mode.

### 3. Ollama and Easy RAG configuration

Ollama and Easy RAG settings are in `src/main/resources/application.properties`. At minimum you need a running Ollama instance, the chat and embedding models pulled, and documents under `src/main/resources/rag/`.

For the full property list, setup steps, tuning options (`max-segment-size`, `max-overlap-size`, embedding reuse), and guidance on when Easy RAG is preferable to a vector database, see **[docs/easy-rag.md](docs/easy-rag.md)**.

### 4. Travel agent API

With the app running in dev mode, send a plain-text question to the travel agent:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "What travel packages do you offer?"
```

The agent uses Easy RAG to answer from documents in `src/main/resources/rag/` (for example, `plans-travel.md` with package details). See [docs/easy-rag.md](docs/easy-rag.md) for how ingestion and retrieval are configured.

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
├── docs/
│   └── easy-rag.md                  # Easy RAG configuration and usage guide
├── pom.xml
├── src/main/java/com/mathffreitas/
│   ├── TravelAgentAssistent.java    # LangChain4j AI service interface
│   └── TravelAgentResource.java     # REST endpoint (POST /travel)
├── src/main/docker/                 # Docker build files
└── src/main/resources/
    ├── application.properties       # Ollama / LangChain4j configuration
    └── rag/
        └── plans-travel.md          # Travel package knowledge base for Easy RAG
```

## Related documentation

- [Easy RAG configuration (this project)](docs/easy-rag.md)
- [Quarkus guides](https://quarkus.io/guides/)
- [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus LangChain4j — Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)

## Author

Matheus Filipe Freitas
