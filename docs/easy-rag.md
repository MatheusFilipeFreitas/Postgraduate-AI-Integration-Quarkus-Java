# Easy RAG configuration

This project uses [Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html) from the Quarkus LangChain4j extension to give the travel agent access to a local knowledge base without setting up a vector database or writing a custom RAG pipeline.

On startup, Quarkus reads documents from a configured directory, splits them into segments, computes embeddings via Ollama, and stores them in an **in-memory embedding store**. When a user calls `POST /travel`, LangChain4j retrieves the most relevant segments and injects them into the chat model prompt automatically.

## How it works in this project

```
src/main/resources/rag/*.md
        │
        ▼  (startup ingestion)
  Document splitter  ──►  Ollama embeddings (nomic-embed-text)
        │
        ▼
  In-memory embedding store
        │
        ▼  (on each /travel request)
  Retrieval augmentor  ──►  Ollama chat model (gemma3:4b)  ──►  response
```

No `@RegisterAiService` customization is required. Adding the `quarkus-langchain4j-easy-rag` dependency and the configuration below is enough for RAG to be wired to `TravelAgentAssistent.chat()`.

## What you need to configure

### 1. Maven dependencies

Both extensions must be present in `pom.xml`:

| Dependency | Role |
|------------|------|
| `quarkus-langchain4j-ollama` | Chat model and embedding model via Ollama |
| `quarkus-langchain4j-easy-rag` | Automatic document ingestion, in-memory store, and retrieval augmentor |

### 2. Ollama

Install and run [Ollama](https://ollama.com/download), then pull the models used by this project:

```shell
ollama pull gemma3:4b
ollama pull nomic-embed-text
```

- **Chat model** (`gemma3:4b`): generates the final answer.
- **Embedding model** (`nomic-embed-text`): converts document segments into vectors for similarity search.

If Ollama runs on another host or port, update `quarkus.langchain4j.ollama.base-url`.

### 3. Knowledge base directory

Place documents under `src/main/resources/rag/`. Supported formats include plain text, Markdown, PDF, DOCX, HTML, and others handled by [Apache Tika](https://tika.apache.org/) (used internally by Easy RAG).

This project ships with `plans-travel.md`, which describes travel packages the agent can quote.

After adding or changing files, restart the application so ingestion runs again.

### 4. Application properties

The active configuration lives in `src/main/resources/application.properties`:

```properties
# Ollama connection
quarkus.langchain4j.ollama.base-url=http://localhost:11434/
quarkus.langchain4j.ollama.chat-model.model-id=gemma3:4b
quarkus.langchain4j.ollama.timeout=60s

# Easy RAG — document source
quarkus.langchain4j.easy-rag.path=src/main/resources/rag

# Embedding provider (required when multiple embedding backends exist)
quarkus.langchain4j.embedding-model.provider=ollama
quarkus.langchain4j.ollama.embedding-model.model-name=nomic-embed-text

# Document splitting (tuning retrieval granularity)
quarkus.langchain4j.easy-rag.max-segment-size=100
quarkus.langchain4j.easy-rag.max-overlap-size=20
```

#### Property reference

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `quarkus.langchain4j.easy-rag.path` | **Yes** | — | Directory with documents to ingest. Relative paths resolve from the process working directory at runtime. |
| `quarkus.langchain4j.embedding-model.provider` | When multiple providers exist | — | Selects Ollama as the embedding backend (`ollama`). |
| `quarkus.langchain4j.ollama.embedding-model.model-name` | **Yes** (with Ollama) | — | Embedding model name in Ollama (`nomic-embed-text`). |
| `quarkus.langchain4j.easy-rag.max-segment-size` | No | `300` | Maximum segment length in **tokens**. Smaller values yield more focused chunks; larger values keep more context per chunk. |
| `quarkus.langchain4j.easy-rag.max-overlap-size` | No | `30` | Token overlap between adjacent segments. Helps avoid cutting sentences or facts across chunk boundaries. |
| `quarkus.langchain4j.easy-rag.max-results` | No | `5` | Number of segments retrieved per query. |
| `quarkus.langchain4j.easy-rag.min-score` | No | none | Minimum similarity score; results below this threshold are discarded. |
| `quarkus.langchain4j.easy-rag.path-matcher` | No | `glob:**` | Which files under `path` to ingest (Java `FileSystem` glob syntax). |
| `quarkus.langchain4j.easy-rag.recursive` | No | `true` | Whether to scan subdirectories. |
| `quarkus.langchain4j.easy-rag.path-type` | No | filesystem | Set to `CLASSPATH` to load documents from the classpath instead of the filesystem. |
| `quarkus.langchain4j.easy-rag.ingestion-strategy` | No | `ON` | `ON` (ingest at startup), `OFF` (skip ingestion), or `MANUAL` (call `EasyRagManualIngestion` yourself). |
| `quarkus.langchain4j.easy-rag.reuse-embeddings.enabled` | No | `false` | Cache computed embeddings to disk between dev restarts (see below). |

Environment variables follow the Quarkus convention, for example `QUARKUS_LANGCHAIN4J_EASY_RAG_PATH` for `quarkus.langchain4j.easy-rag.path`.

### 5. Optional: faster local development

Recomputing embeddings on every `./mvnw quarkus:dev` restart can be slow. Enable embedding reuse:

```properties
quarkus.langchain4j.easy-rag.reuse-embeddings.enabled=true
# quarkus.langchain4j.easy-rag.reuse-embeddings.file=easy-rag-embeddings.json
```

On first startup, embeddings are written to `easy-rag-embeddings.json` in the working directory. Later restarts load from that file instead of calling Ollama again. Delete the file after changing documents or embedding settings.

### 6. Verify it works

With the app running:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -d "What travel packages do you offer?"
```

The response should reflect content from `src/main/resources/rag/plans-travel.md`. You can also use the LangChain4j chat UI in [Quarkus Dev UI](http://localhost:8080/q/dev-ui) while in dev mode.

## Easy RAG vs. RAG with a vector database

Both approaches retrieve relevant document chunks and pass them to an LLM. The difference is how much infrastructure and code you manage.

| Aspect | Easy RAG (this project) | RAG + vector database |
|--------|-------------------------|------------------------|
| Setup | One Maven dependency + `easy-rag.path` | Embedding store extension (Redis, pgvector, Chroma, etc.), ingestion code or jobs, often a custom `RetrievalAugmentor` |
| Storage | In-memory (lost on restart) | Persistent, shared across instances |
| Scale | Small corpora, single instance | Large corpora, many documents, high query volume |
| Document updates | Restart app (or manual ingestion) | Incremental upsert/delete APIs, scheduled re-indexing |
| Operations | No extra services | Database provisioning, backups, monitoring |
| Native image | **Not supported** by Easy RAG | Depends on the chosen store extension |

### When Easy RAG is the better choice

Use Easy RAG when:

- You are **learning RAG** or prototyping, and want results quickly with minimal boilerplate.
- The knowledge base is **small and mostly static** (a few Markdown files, a product catalog, internal FAQs).
- Traffic is **low to moderate** and runs on a **single application instance**.
- Documents change **infrequently**, and a restart (or dev-mode reload) to re-ingest is acceptable.
- You want to **avoid operating** Redis, PostgreSQL + pgvector, or another vector store.
- You are in **local or course/demo** environments where persistence across restarts is not critical.

This travel agency demo fits that profile: a handful of package descriptions, one Ollama instance, and no production multi-node deployment.

### When to move to RAG + vector database

Switch to a dedicated vector store when:

- The corpus is **large** (thousands of files or millions of chunks) and in-memory search becomes slow or memory-heavy.
- You run **multiple replicas** that must share the same index.
- Documents are **updated often** and you need incremental indexing without full restarts.
- You need **durability** — embeddings must survive deploys and restarts.
- You require **advanced retrieval** (metadata filters, hybrid search, reranking pipelines) beyond Easy RAG defaults.
- You are targeting **native executables** (Easy RAG does not support native compilation).

Quarkus LangChain4j supports persistent stores via extensions such as `quarkus-langchain4j-redis` or pgvector integrations. You can keep Ollama for chat and embeddings while replacing only the in-memory store.

## Limitations to keep in mind

- **In-memory store**: All embeddings live in JVM heap. They are rebuilt on every cold start unless you use `reuse-embeddings` (dev convenience only) or plug in a persistent store.
- **No native mode**: Do not rely on Easy RAG for `-Dnative` builds; use a manual RAG setup with a supported store instead.
- **Working directory**: `easy-rag.path=src/main/resources/rag` works in dev because Maven runs from the project root. For packaged JARs, use an absolute path, copy documents to a known location, or set `path-type=CLASSPATH`.
- **Single-process retrieval**: There is no shared index between application instances.

## Further reading

- [Quarkus LangChain4j — Easy RAG](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag-easy-rag.html)
- [Quarkus LangChain4j — RAG overview](https://docs.quarkiverse.io/quarkus-langchain4j/dev/rag.html)
- [LangChain4j — Retrieval-augmented generation](https://docs.langchain4j.dev/tutorials/rag)
