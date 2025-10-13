// ============================================
// Neo4j Index and Constraint Definitions
// ============================================
// This file defines all indexes and constraints for the Memory Graph database
// Run these commands once when initializing the database

// ============================================
// UNIQUE CONSTRAINTS (também criam índices)
// ============================================

// Memory node constraints
CREATE CONSTRAINT memory_id_unique IF NOT EXISTS
FOR (m:Memory) REQUIRE m.id IS UNIQUE;

CREATE CONSTRAINT memory_name_unique IF NOT EXISTS
FOR (m:Memory) REQUIRE m.name IS UNIQUE;

// Observation node constraints
CREATE CONSTRAINT observation_id_unique IF NOT EXISTS
FOR (o:Observation) REQUIRE o.id IS UNIQUE;

// ============================================
// VECTOR INDEXES para busca por similaridade
// ============================================

// Vector index para embeddings de Memory
// Dimensões: ajustar conforme o modelo de embedding usado (ex: 4096 para QWen3 8B, 768 para BERT)
// Similarity: COSINE (conforme usado nas queries)
CREATE VECTOR INDEX memory_embedding_index IF NOT EXISTS
FOR (m:Memory) ON (m.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 4096,
    `vector.similarity_function`: 'cosine'
  }
};

// Vector index para embeddings de Observation
CREATE VECTOR INDEX observation_embedding_index IF NOT EXISTS
FOR (o:Observation) ON (o.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 4096,
    `vector.similarity_function`: 'cosine'
  }
};

// ============================================
// ÍNDICES ADICIONAIS para performance
// ============================================

// Índice no campo description caso seja usado em buscas full-text no futuro
// CREATE INDEX memory_description_index IF NOT EXISTS
// FOR (m:Memory) ON (m.description);

// ============================================
// VERIFICAR ÍNDICES CRIADOS
// ============================================
// Execute este comando para verificar todos os índices:
// SHOW INDEXES;
// SHOW CONSTRAINTS;
