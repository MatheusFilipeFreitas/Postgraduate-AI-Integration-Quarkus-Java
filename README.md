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
| Build tool | Maven |

## Prerequisites

- JDK 25+
- [Ollama](https://ollama.com/download) installed and running locally
- A compatible model pulled in Ollama (for example: `ollama pull llama3.2`)

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

### 3. Configure Ollama (when AI endpoints are added)

Add the following to `src/main/resources/application.properties`:

```properties
quarkus.langchain4j.ollama.base-url=http://localhost:11434
quarkus.langchain4j.ollama.chat-model.model-id=llama3.2
```

Adjust `model-id` to match the model you pulled with Ollama.

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
├── pom.xml                          # Maven build and dependencies
├── src/main/docker/                 # Docker build files
└── src/main/resources/              # Application configuration (to be added)
```

## Related documentation

- [Quarkus guides](https://quarkus.io/guides/)
- [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [LangChain4j Ollama integration](https://docs.langchain4j.dev/integrations/language-models/ollama)

## Author

Matheus Filipe Freitas
