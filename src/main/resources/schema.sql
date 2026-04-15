CREATE TABLE IF NOT EXISTS project_connection (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    display_name TEXT NOT NULL,
    repo_url TEXT,
    branch_name TEXT,
    local_path TEXT,
    workspace_path TEXT,
    gitlab_credential_ref TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS project_index (
    project_id TEXT PRIMARY KEY,
    stack TEXT,
    payload_json TEXT NOT NULL,
    last_indexed_at TEXT NOT NULL,
    index_version INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS job_status (
    job_id TEXT PRIMARY KEY,
    project_id TEXT,
    type TEXT NOT NULL,
    state TEXT NOT NULL,
    progress INTEGER NOT NULL,
    started_at TEXT NOT NULL,
    ended_at TEXT,
    message TEXT
);

CREATE TABLE IF NOT EXISTS conversation_summary (
    conversation_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    summary_json TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
