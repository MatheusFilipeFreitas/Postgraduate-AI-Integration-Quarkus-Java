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
| RAG | Easy RAG (`quarkus-langchain4j-easy-rag`) |
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

### 3. Ollama configuration

Ollama settings are defined in `src/main/resources/application.properties`:

```properties
quarkus.langchain4j.ollama.base-url=http://localhost:11434/
quarkus.langchain4j.ollama.chat-model.model-id=gemma3:4b
quarkus.langchain4j.ollama.timeout=60s

quarkus.langchain4j.easy-rag.path=src/main/resources/rag
quarkus.langchain4j.embedding-model.provider=ollama
quarkus.langchain4j.ollama.embedding-model.model-name=nomic-embed-text
```

| Property | Description |
|----------|-------------|
| `base-url` | Ollama server address (default local instance) |
| `chat-model.model-id` | Chat model invoked by LangChain4j (`gemma3:4b`) |
| `timeout` | Request timeout; fails if the model does not respond within 60s |
| `easy-rag.path` | Directory with documents indexed by Easy RAG |
| `embedding-model.provider` | Embedding backend (`ollama`) |
| `embedding-model.model-name` | Ollama embedding model (`nomic-embed-text`) |

Change `model-id` if you use a different Ollama chat model. Easy RAG with embeddings requires `nomic-embed-text` to be installed in Ollama (`ollama pull nomic-embed-text`).

### 4. Travel agent API

With the app running in dev mode, send a plain-text question to the travel agent:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "What travel packages do you offer?"
```

The agent uses Easy RAG to answer from documents in `src/main/resources/rag/` (for example, `plans-travel.md` with package details).

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

- [Quarkus guides](https://quarkus.io/guides/)
- [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)

## Author

Matheus Filipe Freitas
