#!/usr/bin/env node
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

const API_BASE_URL = process.env.ISTARI_API_URL || "http://localhost:8080";

async function makeRequest(endpoint, method = "GET", body = null) {
  const options = {
    method,
    headers: {
      "Content-Type": "application/json",
    },
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }

  return response.json();
}

const server = new Server(
  {
    name: "istari-mcp",
    version: "0.1.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "create_memory",
        description: "Create a new entity node in the knowledge graph. Use this to store any type of knowledge that can be represented as a distinct entity. Each memory is embedded as a vector for semantic search and can be connected to other memories through relationships.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Unique identifier for the memory entity. Use natural, human-readable names that clearly represent the entity (e.g., 'John Doe', 'ACME Corporation', 'Q4 Planning Meeting', 'Python Programming Language', 'Machine Learning Concept').",
            },
            description: {
              type: "string",
              description: "Core description that captures the essence of what this entity represents (up to 1024 characters). This will be embedded as a vector for semantic similarity search. IMPORTANT: Be comprehensive and detailed - include as much relevant context, identifying information, and defining characteristics as possible. The richer the description, the better the semantic search will perform. Think of this as the primary way to capture the entity's identity and purpose.",
            },
            observations: {
              type: "array",
              items: { type: "string" },
              description: "Optional list of detailed information about this entity. Each observation can contain rich textual content (up to 1024 characters) including facts, attributes, properties, contextual details, or any relevant information. IMPORTANT: Be thorough and comprehensive - each observation should capture substantial information, not just brief facts. Use the full capacity to store nuanced details, behaviors, preferences, historical context, and any information that adds depth to understanding this entity. Each observation is stored separately and embedded as a vector for granular semantic search.",
            },
          },
          required: ["name", "description"],
        },
      },
      {
        name: "get_memory",
        description: "Retrieve a specific memory entity by its exact name. Returns the complete memory including description, all observations, and all relationships (incoming and outgoing) with other entities in the knowledge graph.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Exact unique name of the memory entity to retrieve (case-sensitive).",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "search_memories",
        description: "Perform semantic similarity search across all memory entities and their observations using vector embeddings. Returns ranked results with similarity scores. Use this to find relevant information when you don't know exact entity names or to discover related concepts.",
        inputSchema: {
          type: "object",
          properties: {
            context: {
              type: "string",
              description: "Natural language search query. The query will be embedded as a vector and compared against all memory descriptions and observations to find semantically similar content.",
            },
          },
          required: ["context"],
        },
      },
      {
        name: "update_memory",
        description: "Update the core description of an existing memory entity. This will regenerate the vector embedding for the updated description. Use this when the fundamental nature or context of an entity changes. For adding new facts, use add_observation instead.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Exact unique name of the memory entity to update.",
            },
            description: {
              type: "string",
              description: "New core description that will replace the existing one (up to 1024 characters). IMPORTANT: Be comprehensive and detailed - include as much relevant context, identifying information, and defining characteristics as possible. The richer the description, the better the semantic search will perform. This will be re-embedded as a vector for semantic search.",
            },
          },
          required: ["name", "description"],
        },
      },
      {
        name: "delete_memory",
        description: "Permanently remove a memory entity from the knowledge graph. This will delete the entity node, all its observations, embeddings, and all relationships connected to it. Use with caution as this operation cannot be undone.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Exact unique name of the memory entity to permanently delete.",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "add_observation",
        description: "Add detailed information to an existing memory entity. Observations can contain rich textual content (up to 1024 characters) including facts, attributes, properties, contextual details, or any relevant information. Each observation is stored as a separate node, embedded as a vector, and linked to the parent memory. Use this to incrementally build knowledge about an entity without modifying its core description.",
        inputSchema: {
          type: "object",
          properties: {
            memoryName: {
              type: "string",
              description: "Exact unique name of the memory entity to add the observation to.",
            },
            observation: {
              type: "string",
              description: "Detailed textual content about the entity (up to 1024 characters). IMPORTANT: Be thorough and comprehensive - use the full capacity to capture substantial information, not just brief facts. Include nuanced details, context, reasoning, implications, and any information that adds depth. This will be embedded as a vector and stored as a separate searchable node linked to the parent memory. Examples: 'Prefers asynchronous communication and detailed written documentation over meetings. Has expressed that this stems from working across multiple time zones and finding that written communication creates better documentation trails for future reference. Particularly values RFC-style documents for technical decisions', 'Specializes in distributed systems with focus on event-driven architectures and microservices patterns. Has 8+ years of experience building scalable systems handling millions of requests per day. Strong advocate for observability and has implemented comprehensive monitoring solutions using OpenTelemetry', 'Published in 2023 as a breakthrough paper that introduced novel attention mechanisms for transformer models. The work demonstrated 40% improvement in training efficiency while maintaining model quality. Has been cited over 500 times and influenced several major language model architectures'.",
            },
          },
          required: ["memoryName", "observation"],
        },
      },
      {
        name: "create_relationship",
        description: "Create a directed edge between two memory entities in the knowledge graph. Relationships define how entities are semantically connected and form the structure of the knowledge network. Use this to build a rich semantic graph of interconnected knowledge.",
        inputSchema: {
          type: "object",
          properties: {
            sourceMemoryName: {
              type: "string",
              description: "Exact unique name of the source memory entity (the relationship originates from this entity).",
            },
            targetMemoryName: {
              type: "string",
              description: "Exact unique name of the target memory entity (the relationship points to this entity).",
            },
            relationshipType: {
              type: "string",
              description: "Type of relationship connecting the entities. Use list_relationship_types to see all available types. Common types include: WORKS_AT, KNOWS, HAS, IS_A, PART_OF, RELATED_TO, INVOLVES, MANAGES, REPORTS_TO, etc.",
            },
          },
          required: ["sourceMemoryName", "targetMemoryName", "relationshipType"],
        },
      },
      {
        name: "list_relationship_types",
        description: "Retrieve all valid relationship types that can be used to connect memory entities in the knowledge graph. Use these types when creating relationships to maintain consistency and semantic clarity in the graph structure.",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
    ],
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  try {
    const { name, arguments: args } = request.params;

    switch (name) {
      case "create_memory": {
        const result = await makeRequest("/memories", "POST", {
          name: args.name,
          description: args.description,
          observations: args.observations || [],
        });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "get_memory": {
        const result = await makeRequest(`/memories?name=${encodeURIComponent(args.name)}`);
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "search_memories": {
        const result = await makeRequest(
          `/memories/search?context=${encodeURIComponent(args.context)}`
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "update_memory": {
        const result = await makeRequest("/memories", "PUT", {
          name: args.name,
          description: args.description,
        });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "delete_memory": {
        const result = await makeRequest(
          `/memories?name=${encodeURIComponent(args.name)}`,
          "DELETE"
        );
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "add_observation": {
        const result = await makeRequest("/observations", "POST", {
          memoryName: args.memoryName,
          observation: args.observation,
        });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "create_relationship": {
        const result = await makeRequest("/relationships", "POST", {
          sourceMemoryName: args.sourceMemoryName,
          targetMemoryName: args.targetMemoryName,
          relationshipType: args.relationshipType,
        });
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case "list_relationship_types": {
        const result = await makeRequest("/relationships");
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    return {
      content: [
        {
          type: "text",
          text: `Error: ${error.message}`,
        },
      ],
      isError: true,
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Istari MCP server running on stdio");
}

main().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});

export default server;
