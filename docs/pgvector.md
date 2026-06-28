# pgvector configuration

This project uses [pgvector](https://github.com/pgvector/pgvector) through the [Quarkus LangChain4j PGVector extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html) to persist document embeddings in PostgreSQL and perform similarity search during RAG queries.

Unlike Easy RAG, pgvector does not ingest documents automatically. You run PostgreSQL, configure a datasource, write ingestion code, and wire a custom `RetrievalAugmentor`. That extra setup buys persistent storage, shared indexes across app instances, and more control over splitting and retrieval.

## How it works in this project

```
src/main/resources/rag/plans-travel.md
        │
        ▼  (startup — DocumentIngest.java)
  Document splitter (200 chars, 20 overlap)
        │
        ▼
  Ollama embeddings (nomic-embed-text, 768 dimensions)
        │
        ▼
  PostgreSQL table `travel_embeddings` (pgvector column)
        │
        ▼  (on each /travel request — RagConfiguration.java)
  EmbeddingStoreContentRetriever  ──►  Ollama chat model (gemma3:4b)  ──►  response
```

At a high level:

1. **pgvector** is a PostgreSQL extension that adds a `vector` column type and distance operators (L2, cosine, inner product).
2. **Quarkus LangChain4j** creates an `EmbeddingStore<TextSegment>` bean backed by PostgreSQL. The extension manages the table schema.
3. **`DocumentIngest`** loads `plans-travel.md` from the classpath, splits it into segments, computes embeddings via Ollama, and stores them in PostgreSQL on startup.
4. **`RagConfiguration`** produces a `RetrievalAugmentor` that embeds the user question, runs a nearest-neighbor search in PostgreSQL, and injects the top segments into the prompt sent to `TravelAgentAssistent.chat()`.

## Prerequisites

| Requirement | This project |
|-------------|--------------|
| PostgreSQL with pgvector | `docker compose up -d` (see `docker-compose.yml`) |
| JDBC driver | `quarkus-jdbc-postgresql` |
| Embedding model | Ollama `nomic-embed-text` (768 dimensions) |
| Chat model | Ollama `gemma3:4b` |

Pull the Ollama models before starting the app:

```shell
ollama pull gemma3:4b
ollama pull nomic-embed-text
```

## 1. Start PostgreSQL

The repository ships a Docker Compose file that runs PostgreSQL 16 with pgvector preinstalled:

```shell
docker compose up -d
```

Default connection settings (also used in `application.properties`):

| Setting | Value |
|---------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `travel` |
| User | `travel` |
| Password | `travel` |

In dev mode, Quarkus can also start a PostgreSQL instance automatically via Dev Services when Docker is available. This project uses an explicit `docker-compose.yml` so the database is always the same across environments.

## 2. Maven dependencies

Both extensions must be present in `pom.xml`:

| Dependency | Role |
|------------|------|
| `quarkus-langchain4j-ollama` | Chat model and embedding model via Ollama |
| `quarkus-langchain4j-pgvector` | PostgreSQL-backed `EmbeddingStore` |
| `quarkus-jdbc-postgresql` | JDBC datasource for PostgreSQL |

## 3. Application properties

The active configuration lives in `src/main/resources/application.properties`:

```properties
# Ollama
quarkus.langchain4j.ollama.base-url=http://192.168.68.57:11434/
quarkus.langchain4j.ollama.chat-model.model-id=gemma3:4b
quarkus.langchain4j.ollama.timeout=60s
quarkus.langchain4j.embedding-model.provider=ollama
quarkus.langchain4j.ollama.embedding-model.model-id=nomic-embed-text

# PostgreSQL datasource
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=travel
quarkus.datasource.password=travel
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/travel

# pgvector store
quarkus.langchain4j.pgvector.register-vector-pg-extension=true
quarkus.langchain4j.pgvector.dimension=768
quarkus.langchain4j.pgvector.table=travel_embeddings
quarkus.langchain4j.pgvector.drop-table-first=true
```

### Property reference

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `quarkus.datasource.*` | **Yes** | — | Standard Quarkus JDBC datasource. pgvector uses the default datasource unless `quarkus.langchain4j.pgvector.datasource` is set. |
| `quarkus.langchain4j.pgvector.dimension` | **Yes** | — | Vector size produced by the embedding model. Must match exactly (`nomic-embed-text` → **768**). |
| `quarkus.langchain4j.pgvector.table` | No | `embeddings` | PostgreSQL table name for stored vectors. |
| `quarkus.langchain4j.pgvector.register-vector-pg-extension` | No | `false` (overridden to `true` in dev/test) | Runs `CREATE EXTENSION vector` on startup when the DB user has permission. |
| `quarkus.langchain4j.pgvector.create-table` | No | `true` | Creates the embeddings table if it does not exist. |
| `quarkus.langchain4j.pgvector.drop-table-first` | No | `false` | Drops the table before creating it. Useful in dev; **do not enable in production**. |
| `quarkus.langchain4j.pgvector.use-index` | No | `false` | Enables an IVFFlat index for faster search on large datasets. |
| `quarkus.langchain4j.pgvector.index-list-size` | No | `0` | IVFFlat cluster count. Required when `use-index=true`. |
| `quarkus.langchain4j.embedding-model.provider` | When multiple providers exist | — | Selects Ollama as the embedding backend. |

Environment variables follow the Quarkus convention, for example `QUARKUS_LANGCHAIN4J_PGVECTOR_DIMENSION` for `quarkus.langchain4j.pgvector.dimension`.

### Embedding dimension

The dimension **must** match the embedding model output. A mismatch causes ingestion or retrieval failures, or silently poor results.

| Embedding model | Dimension |
|-----------------|-----------|
| `nomic-embed-text` (this project) | 768 |
| `AllMiniLmL6V2` | 384 |
| OpenAI `text-embedding-ada-002` | 1536 |

If you change the embedding model, update `quarkus.langchain4j.pgvector.dimension` and re-ingest all documents.

## 4. Document ingestion

Ingestion is implemented in `DocumentIngest.java`:

```java
Document document = ClassPathDocumentLoader.loadDocument("rag/plans-travel.md");
document.metadata().put("type", "packages");

DocumentSplitter splitter = DocumentSplitters.recursive(200, 20);

EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(splitter)
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();

ingestor.ingest(document);
```

Key points:

- Documents are loaded from the **classpath** (`src/main/resources/rag/`), which works in dev mode and packaged JARs.
- Splitting uses **character-based** recursive splitting (200 max segment size, 20 overlap). Easy RAG defaults to **token-based** splitting via configuration properties.
- Metadata (for example `type=packages`) is stored alongside each segment in PostgreSQL and can be used for filtered retrieval in more advanced setups.
- Ingestion runs once on startup via `@Observes StartupEvent`. Adding or changing documents requires a restart (or a separate re-ingestion job).

Each ingested segment becomes a row in `travel_embeddings` containing the text, optional metadata (JSON), and the vector embedding.

## 5. Retrieval augmentor

Easy RAG wires retrieval automatically. With pgvector, you provide a CDI producer in `RagConfiguration.java`:

```java
@Produces
public RetrievalAugmentor retrievalAugmentor() {
    return DefaultRetrievalAugmentor.builder()
            .contentRetriever(
                    EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(5)
                            .build())
            .build();
}
```

On each request, LangChain4j:

1. Embeds the user question with `nomic-embed-text`.
2. Runs a similarity search in PostgreSQL (`ORDER BY embedding <=> query_vector LIMIT 5`).
3. Injects the matching segments into the prompt for `TravelAgentAssistent`.

The `@RegisterAiService` interface does not need changes; Quarkus picks up the produced `RetrievalAugmentor` automatically.

## 6. Verify it works

Start PostgreSQL, then run the app:

```shell
docker compose up -d
./mvnw quarkus:dev
```

Check the logs for `Document ingested!` and `>>> Creating RetrievalAugmentor`.

Test the API:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "What travel packages do you offer?"
```

The response should reflect content from `plans-travel.md`. You can also inspect the database:

```shell
docker compose exec postgres psql -U travel -d travel -c "SELECT COUNT(*) FROM travel_embeddings;"
```

## Production considerations

| Topic | Recommendation |
|-------|----------------|
| `drop-table-first` | Set to `false` in production to avoid wiping embeddings on every deploy. |
| Indexing | Enable `use-index` and tune `index-list-size` when the table grows beyond a few thousand rows. |
| Re-ingestion | Plan incremental upserts or scheduled jobs instead of full startup ingestion. |
| Credentials | Externalize datasource username/password via env vars or secrets. |
| Backups | Include the embeddings table in regular PostgreSQL backups. |
| Multiple instances | All replicas share the same PostgreSQL index — no per-JVM duplication. |

## Further reading

- [PGVector Document Store (Quarkus LangChain4j)](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-pgvector-store.html)
- [pgvector GitHub repository](https://github.com/pgvector/pgvector)
- [Easy RAG vs pgvector in this project](easy-rag-vs-pgvector.md)
- [LangChain4j — Retrieval-augmented generation](https://docs.langchain4j.dev/tutorials/rag)
