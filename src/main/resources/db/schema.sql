-- 创建容器表
CREATE TABLE IF NOT EXISTS containers
(
    id
    TEXT
    PRIMARY
    KEY,
    name
    TEXT
    NOT
    NULL,
    image
    TEXT
    NOT
    NULL,
    status
    TEXT
    NOT
    NULL,
    ports
    TEXT,
    created
    TEXT
    NOT
    NULL,
    updated
    TEXT
    NOT
    NULL
);

-- 创建模板表
CREATE TABLE IF NOT EXISTS templates
(
    id
    TEXT
    PRIMARY
    KEY,
    name
    TEXT
    NOT
    NULL,
    description
    TEXT,
    base_image
    TEXT
    NOT
    NULL,
    env_config
    TEXT,
    port_config
    TEXT,
    created
    TEXT
    NOT
    NULL,
    updated
    TEXT
    NOT
    NULL
);

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_logs
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    operation
    TEXT
    NOT
    NULL,
    operator
    TEXT
    NOT
    NULL,
    status
    TEXT
    NOT
    NULL,
    details
    TEXT,
    timestamp
    TEXT
    NOT
    NULL
);

-- 创建系统日志表
CREATE TABLE IF NOT EXISTS system_logs
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    level
    TEXT
    NOT
    NULL,
    content
    TEXT
    NOT
    NULL,
    timestamp
    TEXT
    NOT
    NULL
);
-- 镜像状态表
CREATE TABLE IF NOT EXISTS image_status
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    name
    TEXT
    NOT
    NULL, -- 仓库名，如 jxxghp/moviepilot-v2
    tag
    TEXT
    NOT
    NULL, -- 镜像标签，如 latest、v1.0.0
    local_create_time
    TEXT, -- 本地镜像创建时间
    remote_create_time
    TEXT, -- 远程镜像创建时间
    need_update
    INTEGER
    DEFAULT
    0,    -- 0 表示 false，1 表示 true
    last_checked
    TEXT
    DEFAULT (
    datetime
(
    'now'
)), -- 检查时间，ISO8601格式
    created_at TEXT DEFAULT
(
    datetime
(
    'now'
)),
    updated_at TEXT DEFAULT
(
    datetime
(
    'now'
)),
    UNIQUE
(
    name,
    tag
)
    );
CREATE TABLE IF NOT EXISTS system_settings
(
    setting_key
    VARCHAR
(
    100
) PRIMARY KEY,
    setting_value TEXT
    );
-- 创建索引
CREATE INDEX IF NOT EXISTS idx_containers_name ON containers(name);
CREATE INDEX IF NOT EXISTS idx_templates_name ON templates(name);
CREATE INDEX IF NOT EXISTS idx_operation_logs_timestamp ON operation_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_system_logs_timestamp ON system_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_system_logs_level ON system_logs(level); 