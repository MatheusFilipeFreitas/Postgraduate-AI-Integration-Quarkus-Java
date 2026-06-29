# MCP configuration

This project consumes booking tools from a separate [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) over the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/). Travel package answers still come from pgvector RAG; reservation lookup and cancellation are delegated to the MCP server.

Splitting booking into its own MCP service keeps domain logic reusable and lets the travel agent call remote tools without embedding booking code in this repository.

## Architecture

```
Customer  ──►  POST /travel  ──►  TravelAgentAssistent
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
                    ▼                   ▼                   ▼
              RAG (pgvector)     MCP client            Ollama chat
              travel packages    booking-server         gemma3:4b
                    │                   │
                    │                   ▼
                    │         http://localhost:8081/mcp/sse
                    │                   │
                    │                   ▼
                    │         booking-mcp-server
                    │           ├── getBookingDetails
                    │           └── cancelBooking
                    │
                    └──────►  combined answer to customer
```

| Service | Port | Responsibility |
|---------|------|----------------|
| **travel-agency** (this repo) | `8080` | AI agent, RAG, MCP **client**, guardrails |
| [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java) | `8081` | MCP **server**, booking tools, in-memory mock data |

**Start booking-mcp-server before travel-agency** so tools are available when the agent handles booking questions.

## How it works in this project

1. **`quarkus-langchain4j-mcp`** adds MCP client support to the Quarkus LangChain4j extension.
2. **`application.properties`** configures a named client `booking-server` pointing at the SSE endpoint.
3. **`TravelAgentAssistent`** declares `@McpToolBox("booking-server")` so the LLM can discover and invoke remote tools during `chat()`.
4. When a customer asks about a booking, the model calls MCP tools instead of inventing reservation data.

Wiring in code:

```java
@RegisterAiService
public interface TravelAgentAssistent {

    @McpToolBox("booking-server")
    @InputGuardrails(InjectionGuard.class)
    @OutputGuardrails(ToneGuardrail.class)
    String chat(@MemoryId String memoryId,
                @V("userMessage") String userMessage,
                @V("username") String username);
}
```

The MCP client name in `@McpToolBox` must match the configuration prefix in `application.properties`.

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| booking-mcp-server running | `./mvnw quarkus:dev` in the sibling project |
| MCP endpoint reachable | Default `http://localhost:8081/mcp/sse` |
| Maven dependency | `quarkus-langchain4j-mcp` (already in `pom.xml`) |
| Ollama chat model | `gemma3:4b` — must support tool calling for MCP tools to work reliably |

## Configuration

In `src/main/resources/application.properties`:

```properties
# MCP client — booking-server tools served by booking-mcp-server (port 8081)
quarkus.langchain4j.mcp.booking-server.transport-type=http
quarkus.langchain4j.mcp.booking-server.url=http://localhost:8081/mcp/sse
quarkus.langchain4j.mcp.booking-server.log-requests=true
quarkus.langchain4j.mcp.booking-server.log-responses=true
```

| Property | Description |
|----------|-------------|
| `quarkus.langchain4j.mcp.<name>.transport-type` | `http` for legacy HTTP/SSE transport (used with Quarkus MCP server SSE) |
| `quarkus.langchain4j.mcp.<name>.url` | Full SSE URL of the MCP server |
| `quarkus.langchain4j.mcp.<name>.log-requests` | Log MCP requests (useful in dev) |
| `quarkus.langchain4j.mcp.<name>.log-responses` | Log MCP responses (useful in dev) |

Replace `<name>` with `booking-server` to match `@McpToolBox("booking-server")`.

If booking-mcp-server runs on another host or port, update only the URL — no code changes required.

## Available MCP tools

Defined in [booking-mcp-server](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java):

| Tool | Purpose |
|------|---------|
| `getBookingDetails` | Look up a booking by numeric id |
| `cancelBooking` | Cancel a booking after identity verification (booking id + customer name) |

### Mock booking data

The MCP server ships with in-memory sample bookings:

| ID | Customer | Destination | Status |
|----|----------|-------------|--------|
| `12345` | John Doe | Egyptian Treasures | CONFIRMED |
| `67890` | Jane Smith | Amazon Adventure | PENDING |

Cancellation requires the booking id and a name that matches the customer on the booking (e.g. `Doe` for John Doe).

## Local setup

### 1. Clone both repositories

```shell
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java.git booking-mcp-server
git clone git@github.com:MatheusFilipeFreitas/Postgraduate-AI-Integration-Quarkus-Java.git travel-agency
```

### 2. Start the MCP server

```shell
cd booking-mcp-server
./mvnw quarkus:dev
```

Verify:

- HTTP port: `8081`
- MCP SSE: `http://localhost:8081/mcp/sse`
- Dev UI: [http://localhost:8081/q/dev/](http://localhost:8081/q/dev/) — use the **MCP Server** card to inspect and call tools

### 3. Start travel-agency

With PostgreSQL and Ollama already running (see [pgvector.md](pgvector.md)):

```shell
cd travel-agency
./mvnw quarkus:dev
```

## Testing through the travel agent

Use the REST API — the agent decides when to call MCP tools:

```shell
# Booking lookup
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -H "X-User-Name: Jane Smith" \
  -d "What are the details of booking 12345?"

# Booking cancellation
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -H "X-User-Name: John Doe" \
  -d "Please cancel booking 12345 for customer Doe."
```

Package questions still use RAG only — MCP tools are invoked for reservation-related requests:

```shell
curl -X POST http://localhost:8080/travel \
  -H "Content-Type: text/plain" \
  -H "X-User-Name: Guest" \
  -d "What travel packages do you offer?"
```

Enable `quarkus.langchain4j.mcp.booking-server.log-requests=true` and `log-responses=true` to trace MCP traffic in the application log.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Agent cannot answer booking questions | booking-mcp-server not running | Start MCP server on port `8081` |
| Connection refused on MCP calls | Wrong URL or port | Check `quarkus.langchain4j.mcp.booking-server.url` |
| No tools available to the model | Client name mismatch | Ensure `@McpToolBox("booking-server")` matches property prefix |
| Model ignores tools | Weak tool-calling support | Try a model with better tool support; `gemma3:4b` may be limited |
| Booking not found | Invalid mock id | Use `12345` or `67890` from mock data |

## Production notes

- Run booking-mcp-server as a separate deployable service; point the URL at your internal MCP endpoint.
- Replace in-memory mock data in booking-mcp-server with a real persistence layer when moving beyond demos.
- Consider TLS and network policies between travel-agency and the MCP server in production.
- Monitor MCP latency — each tool call adds a round trip before the LLM completes its answer.

## Related documentation

- [booking-mcp-server README](https://github.com/MatheusFilipeFreitas/Postgraduate-MCP-Travel-Agent-Quarkus-Java/blob/main/README.md)
- [Guardrails (this project)](guardrails.md)
- [pgvector configuration (this project)](pgvector.md)
- [Quarkus LangChain4j — MCP integration](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [Quarkus MCP Server](https://docs.quarkiverse.io/quarkus-mcp-server/dev/index.html)
- [Model Context Protocol](https://modelcontextprotocol.io/)
