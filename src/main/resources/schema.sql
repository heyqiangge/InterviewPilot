CREATE TABLE IF NOT EXISTS sessions (
  id VARCHAR(36) PRIMARY KEY,
  candidate_id VARCHAR(100) NOT NULL,
  target_role VARCHAR(200) NOT NULL,
  current_question CLOB NOT NULL,
  current_topic VARCHAR(100) NOT NULL,
  current_memory_ids CLOB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS turns (
  id VARCHAR(36) PRIMARY KEY,
  session_id VARCHAR(36) NOT NULL,
  question CLOB NOT NULL,
  topic VARCHAR(100) NOT NULL,
  answer CLOB NOT NULL,
  score INT NOT NULL,
  feedback CLOB NOT NULL,
  retrieved_memory_ids CLOB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  FOREIGN KEY (session_id) REFERENCES sessions(id)
);
CREATE TABLE IF NOT EXISTS memories (
  id VARCHAR(36) PRIMARY KEY,
  candidate_id VARCHAR(100) NOT NULL,
  topic VARCHAR(100) NOT NULL,
  kind VARCHAR(30) NOT NULL,
  fact CLOB NOT NULL,
  importance DOUBLE PRECISION NOT NULL,
  confidence DOUBLE PRECISION NOT NULL,
  source_turn_id VARCHAR(36) NOT NULL,
  active BOOLEAN DEFAULT TRUE NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_memory_candidate ON memories(candidate_id, active);
