# Easy RAG vs pgvector

Both approaches implement **Retrieval-Augmented Generation (RAG)**: relevant document chunks are retrieved and passed to an LLM so answers are grounded in your knowledge base. The difference is how much infrastructure and code you manage, and what you gain in return.

This project currently uses **pgvector**. The [Easy RAG guide](easy-rag.md) documents the earlier, simpler setup that lived entirely in configuration.

## Side-by-side overview

| Aspect | Easy RAG | pgvector (this project) |
|--------|----------|-------------------------|
| **Maven dependency** | `quarkus-langchain4j-easy-rag` | `quarkus-langchain4j-pgvector` + `quarkus-jdbc-postgresql` |
| **External services** | Ollama only | Ollama + PostgreSQL with pgvector |
| **Embedding storage** | In-memory (JVM heap) | PostgreSQL table (`travel_embeddings`) |
| **Document ingestion** | Automatic at startup from a configured directory | Manual code (`DocumentIngest.java`) |
| **Retrieval wiring** | Automatic (extension provides `RetrievalAugmentor`) | Manual CDI producer (`RagConfiguration.java`) |
| **Configuration** | `quarkus.langchain4j.easy-rag.*` properties | Datasource + `quarkus.langchain4j.pgvector.*` properties |
| **Persistence** | Lost on restart (optional dev cache file) | Survives restarts and deploys |
| **Multi-instance** | Each JVM has its own index | Shared index in PostgreSQL |
| **Native image** | Not supported | Supported (depends on store extension) |
| **Setup effort** | Low | Medium |

## Architecture comparison

### Easy RAG

```
src/main/resources/rag/*.md
        │
        ▼  (automatic startup ingestion)
  Easy RAG extension
    ├── document splitter (configurable via properties)
    ├── Ollama embeddings
    └── in-memory embedding store
        │
        ▼  (automatic on each request)
  Retrieval augmentor  ──►  chat model  ──►  response
```

Everything is handled by the extension. Adding the dependency and setting `quarkus.langchain4j.easy-rag.path` is enough.

### pgvector (current)

```
src/main/resources/rag/plans-travel.md
        │
        ▼  (DocumentIngest.java on startup)
  Document splitter  ──►  Ollama embeddings  ──►  PostgreSQL (pgvector)
        │
        ▼  (RagConfiguration.java on each request)
  EmbeddingStoreContentRetriever  ──►  chat model  ──►  response
```

You own ingestion and retrieval wiring. PostgreSQL replaces the in-memory store.

## What changes in code

### Easy RAG — minimal Java

No ingestion or retrieval classes are required. Configuration in `application.properties` is sufficient:

```properties
quarkus.langchain4j.easy-rag.path=src/main/resources/rag
quarkus.langchain4j.easy-rag.max-segment-size=100
quarkus.langchain4j.easy-rag.max-overlap-size=20
```

The AI service interface stays the same:

```java
@RegisterAiService
public interface TravelAgentAssistent {
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
```

### pgvector — explicit pipeline

Three pieces of Java code replace Easy RAG's automation:

| Class | Responsibility |
|-------|----------------|
| `DocumentIngest` | Load documents, split, embed, persist to PostgreSQL on startup |
| `RagConfiguration` | Produce a `RetrievalAugmentor` with `EmbeddingStoreContentRetriever` |
| `TravelAgentAssistent` | Unchanged — still a `@RegisterAiService` interface |

Plus PostgreSQL configuration and a running database (`docker compose up -d`).

## Configuration comparison

### Easy RAG properties (historical)

```properties
quarkus.langchain4j.easy-rag.path=src/main/resources/rag
quarkus.langchain4j.easy-rag.max-segment-size=100
quarkus.langchain4j.easy-rag.max-overlap-size=20
quarkus.langchain4j.easy-rag.max-results=5
quarkus.langchain4j.easy-rag.reuse-embeddings.enabled=true   # optional dev cache
```

Splitting, ingestion timing, path matching, and max results are all property-driven.

### pgvector properties (current)

```properties
# Datasource
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/travel
quarkus.datasource.username=travel
quarkus.datasource.password=travel

# Vector store
quarkus.langchain4j.pgvector.dimension=768
quarkus.langchain4j.pgvector.table=travel_embeddings
quarkus.langchain4j.pgvector.register-vector-pg-extension=true
quarkus.langchain4j.pgvector.drop-table-first=true
```

Splitting (`DocumentSplitters.recursive(200, 20)`) and `maxResults(5)` live in Java, not properties. See [pgvector.md](pgvector.md) for the full property reference.

## When to use each

### Choose Easy RAG when

- You are **learning RAG** or building a quick prototype.
- The knowledge base is **small and mostly static** (a few Markdown files).
- You run a **single instance** with low traffic.
- You want **zero database operations** — only Ollama.
- Restarts that re-ingest documents (or a dev embedding cache file) are acceptable.
- You do **not** need native compilation.

Easy RAG is ideal for course demos and local experiments where persistence does not matter.

### Choose pgvector when

- Embeddings must **survive restarts and deploys**.
- You run **multiple app replicas** that need a shared index.
- The corpus may **grow** beyond what fits comfortably in JVM memory.
- You need **incremental updates**, metadata filters, or custom retrieval logic.
- You want **production-grade** storage with backups and monitoring.
- You may target **native executables** (Easy RAG does not support `-Dnative`).

This travel agency project moved to pgvector to demonstrate a production-oriented RAG pipeline while keeping Ollama for both chat and embeddings.

## Operational trade-offs

| Concern | Easy RAG | pgvector |
|---------|----------|----------|
| Cold start time | Re-embeds all documents (slow without `reuse-embeddings`) | Reads existing rows from PostgreSQL (fast) |
| Memory usage | All vectors in heap | Vectors in PostgreSQL; JVM holds only query results |
| Document updates | Restart app or trigger manual ingestion | Re-run ingestion job or upsert API |
| Dev friction | Lowest — one property for the document path | Requires Docker/PostgreSQL and two Java classes |
| Observability | Limited to app logs | SQL queries, row counts, PostgreSQL metrics |

## Switching between them

To go from Easy RAG to pgvector (as this project did):

1. Remove `quarkus-langchain4j-easy-rag` from `pom.xml`.
2. Add `quarkus-langchain4j-pgvector` and `quarkus-jdbc-postgresql`.
3. Configure the datasource and pgvector properties.
4. Add `DocumentIngest` and `RagConfiguration` classes.
5. Start PostgreSQL (`docker compose up -d`).

To revert to Easy RAG:

1. Remove pgvector dependencies and Java classes.
2. Add `quarkus-langchain4j-easy-rag` back.
3. Restore `quarkus.langchain4j.easy-rag.*` properties.
4. Remove PostgreSQL configuration (and stop the container if no longer needed).

Ollama settings (`base-url`, chat model, embedding model) stay the same in both setups.

## Further reading

- [Easy RAG configuration (earlier setup)](easy-rag.md)
- [pgvector configuration (current setup)](pgvector.md)
- [Quarkus LangChain4j — Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html)
- [Quarkus LangChain4j — PGVector Document Store](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
