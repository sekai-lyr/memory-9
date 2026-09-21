CREATE TABLE IF NOT EXISTS sekai_form_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sekai_form_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    model_type VARCHAR(20),
    model_path VARCHAR(500),
    satiety INT DEFAULT 50,
    mood INT DEFAULT 50,
    affection INT DEFAULT 0,
    level INT DEFAULT 1,
    exp INT DEFAULT 0,
    free_points INT DEFAULT 0,
    hp INT DEFAULT 100,
    atk INT DEFAULT 20,
    def INT DEFAULT 10,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sekai_form_food (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    satiety_value INT DEFAULT 0,
    mood_value INT DEFAULT 0,
    affection_value INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sekai_form_interaction_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT NOT NULL,
    interaction_type VARCHAR(50),
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS live2d_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT,
    model_name VARCHAR(200),
    model_path VARCHAR(500),
    display_name VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS live2d_dialogue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    category VARCHAR(50),
    text VARCHAR(500),
    motion_name VARCHAR(100),
    expression_name VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS live2d_chat_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    api_url VARCHAR(500),
    api_key VARCHAR(200),
    model_name VARCHAR(100),
    image_model_name VARCHAR(100) DEFAULT 'wanx2.1-t2i-turbo',
    image_api_url VARCHAR(500),
    system_prompt VARCHAR(2000),
    max_tokens INT DEFAULT 500,
    temperature DOUBLE DEFAULT 0.7,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agent runtime state.  These tables deliberately keep conversation identifiers
-- server-side; they are never returned in the public chat response.
CREATE TABLE IF NOT EXISTS agent_conversation (
    conversation_id VARCHAR(200) PRIMARY KEY,
    user_id VARCHAR(200),
    model_id BIGINT,
    session_state VARCHAR(40) DEFAULT 'active',
    rolling_summary CLOB,
    summary_message_count INT DEFAULT 0,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE live2d_chat_config ADD COLUMN IF NOT EXISTS image_model_name VARCHAR(100) DEFAULT 'wanx2.1-t2i-turbo';
ALTER TABLE live2d_chat_config ADD COLUMN IF NOT EXISTS image_api_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content CLOB,
    modality VARCHAR(30) DEFAULT 'TEXT',
    attachment_meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(200),
    user_id VARCHAR(200),
    memory_type VARCHAR(40) DEFAULT 'conversation',
    content CLOB NOT NULL,
    importance DOUBLE DEFAULT 0.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_rag_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(200),
    user_id VARCHAR(200),
    title VARCHAR(300),
    content CLOB NOT NULL,
    metadata CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_execution_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(200),
    phase VARCHAR(40),
    status VARCHAR(30),
    message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
