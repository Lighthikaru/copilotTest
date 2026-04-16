export type ProjectConnection = {
  id: string;
  type: "local";
  displayName: string;
  repoUrl?: string | null;
  branch?: string | null;
  localPath?: string | null;
  workspacePath?: string | null;
  gitlabCredentialRef?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type JobStatus = {
  jobId: string;
  type: string;
  state: string;
  progress: number;
  startedAt: string;
  endedAt?: string | null;
  message: string;
};

export type AuthState = {
  copilotCliReady: boolean;
  copilotLoggedIn: boolean;
  copilotEntitled: boolean;
  availableModels: string[];
  loginCommand: string;
  statusMessage: string;
  javaReady: boolean;
  javaStatus: string;
  javaSource: string;
  copilotStatus: string;
  copilotSource: string;
};

export type HealthState = Record<string, string>;

export type AppSettings = {
  javaPath?: string | null;
  copilotCliPath?: string | null;
  dataRoot?: string | null;
};

export type LocalProjectRequest = {
  displayName: string;
  localPath: string;
};

export type ChatRequest = {
  projectId: string;
  conversationId?: string;
  question: string;
  mode: string;
  selectedModel?: string;
};

export type SourceRef = {
  path: string;
  kind: string;
  reason: string;
  excerpt: string;
};

export type ChatResponse = {
  answer: string;
  sources: SourceRef[];
  confidence: number;
  followUps: string[];
  warnings: string[];
  usage: Record<string, unknown>;
};

export type ConversationMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  timestamp: string;
  sources: SourceRef[];
  model?: string | null;
  sessionId?: string | null;
  usage?: Record<string, unknown>;
};

export type ConversationItem = {
  id: string;
  projectId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  lastModel?: string | null;
  lastMode?: string | null;
  sessionId: string;
  archived: boolean;
  summarized: boolean;
  messageCount: number;
};

export type ConversationDetail = {
  id: string;
  projectId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  lastModel?: string | null;
  lastMode?: string | null;
  sessionId: string;
  archived: boolean;
  summarized: boolean;
  summary?: string | null;
  messages: ConversationMessage[];
};
