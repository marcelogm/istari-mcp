# Istari MCP

A personal study project exploring Model Context Protocol implementation with graph-based memory storage and vector search capabilities.

**Status**: Work in progress

## Overview

Istari MCP is a knowledge graph system that enables AI assistants to build and query a persistent memory through the Model Context Protocol (MCP). The system combines graph-based storage with vector embeddings to provide both structured relationships and semantic search capabilities.

### Key Features

- **Entity-based Memory**: Store knowledge as distinct entities (people, organizations, concepts, events) with rich descriptions
- **Graph Relationships**: Connect entities through typed relationships (WORKS_AT, KNOWS, HAS, IS_A, etc.)
- **Vector Embeddings**: Automatic embedding generation for semantic similarity search
- **Observations**: Incrementally add detailed information to entities without modifying core descriptions
- **Semantic Search**: Find relevant entities and observations using natural language queries

## Tech Stack

- Java 21
- Micronaut 4.9.3
- Neo4j (graph database)
- Ollama (embedding generation)
- Project Reactor (reactive programming)

## Prerequisites

- Java 21+
- Docker and Docker Compose
- Ollama with qwen3-embedding model
- Node.js (for MCP server)

## Setup

1. Clone the repository:

```bash
git clone <repository-url>
cd istari-mcp
```

2. Start Neo4j database:

```bash
docker-compose up -d neo4j
```

Access Neo4j Browser at http://localhost:7474 (credentials: neo4j/password) 3. Install and start Ollama:

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama serve
```

4. Pull the embedding model:

```bash
ollama pull qwen3-embedding:latest
```

5. Run the application:

```bash
./gradlew run
```

The API will be available at http://localhost:8080

## MCP Server Setup

The MCP server provides an interface between AI assistants and the Istari backend through the Model Context Protocol.

### Installation

1. Make the server executable:

```bash
chmod +x mcp-server.js
```

2. Configure your MCP client. For Continue.dev, edit `~/.continue/config.yaml`:

```yaml
mcpServers:
  - name: istari-mcp
    args:
      - /absolute/path/to/istari-mcp/mcp-server.js
    env:
      ISTARI_API_URL: http://localhost:8080
```

3. Restart your MCP client to load the server.

### Available MCP Tools

#### Memory Management

- **`create_memory`** - Create a new entity in the knowledge graph

  - `name` (required): Unique identifier (e.g., "John Doe", "ACME Corporation")
  - `description` (required): Core description up to 1024 chars, used for vector embedding
  - `observations` (optional): Array of detailed information strings (each up to 1024 chars)
  - **Use case**: Store any knowledge entity - people, organizations, concepts, events, etc.

- **`get_memory`** - Retrieve a specific entity by exact name

  - `name` (required): Exact entity name (case-sensitive)
  - **Returns**: Complete memory including description, observations, and all relationships

- **`search_memories`** - Semantic search across all entities and observations

  - `context` (required): Natural language search query
  - **Returns**: Ranked results with similarity scores
  - **Use case**: Discover relevant information without knowing exact entity names

- **`update_memory`** - Update an entity's core description

  - `name` (required): Entity name to update
  - `description` (required): New description (regenerates vector embedding)
  - **Note**: For adding new facts, use `add_observation` instead

- **`delete_memory`** - Permanently remove an entity
  - `name` (required): Entity name to delete
  - **Warning**: Deletes entity, observations, embeddings, and all relationships

#### Observations

- **`add_observation`** - Add detailed information to an entity
  - `memoryName` (required): Target entity name
  - `observation` (required): Detailed content up to 1024 chars
  - **Use case**: Incrementally build knowledge without modifying core description
  - **Note**: Each observation is separately embedded and searchable

#### Relationships

- **`create_relationship`** - Create a directed edge between entities

  - `sourceMemoryName` (required): Source entity name
  - `targetMemoryName` (required): Target entity name
  - `relationshipType` (required): Type of relationship (see `list_relationship_types`)
  - **Use case**: Build semantic connections in the knowledge graph

- **`list_relationship_types`** - List all valid relationship types
  - **Returns**: Available relationship types (WORKS_AT, KNOWS, HAS, IS_A, PART_OF, etc.)

## Usage Examples

### Creating a Person with Observations

```javascript
// Create entity
create_memory({
  name: "Alice Johnson",
  description:
    "Senior software engineer specializing in distributed systems and cloud architecture",
  observations: [
    "Prefers asynchronous communication and detailed written documentation",
    "Has 8+ years of experience building scalable microservices",
  ],
});

// Add more observations later
add_observation({
  memoryName: "Alice Johnson",
  observation:
    "Recently led migration of monolithic system to event-driven architecture",
});
```

### Building Relationships

```javascript
// Create organization
create_memory({
  name: "TechCorp Inc",
  description: "Technology company focused on cloud infrastructure solutions",
});

// Connect person to organization
create_relationship({
  sourceMemoryName: "Alice Johnson",
  targetMemoryName: "TechCorp Inc",
  relationshipType: "WORKS_AT",
});
```

### Semantic Search

```javascript
// Find relevant entities
search_memories({
  context: "engineers who work with distributed systems",
});
// Returns: Alice Johnson with similarity score
```

## Architecture

### Data Model

- **Memory Nodes**: Core entities with name, description, and vector embedding
- **Observation Nodes**: Detailed information linked to memories, each with its own embedding
- **Relationships**: Typed directed edges connecting memories
- **Vector Index**: Enables semantic similarity search across descriptions and observations

### API Endpoints

- `POST /memories` - Create memory
- `GET /memories?name={name}` - Get memory
- `GET /memories/search?context={query}` - Search memories
- `PUT /memories` - Update memory
- `DELETE /memories?name={name}` - Delete memory
- `POST /observations` - Add observation
- `POST /relationships` - Create relationship
- `GET /relationships` - List relationship types
