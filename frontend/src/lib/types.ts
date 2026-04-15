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

export type ModuleCard = {
  name: string;
  kind: string;
  description: string;
  primaryPaths: string[];
  technologies: string[];
  confidence: number;
};

export type EntryPointInfo = {
  name: string;
  path: string;
  type: string;
  description: string;
};

export type RouteInfo = {
  method: string;
  path: string;
  controller: string;
  sourcePath: string;
  description: string;
};

export type ScheduledJobInfo = {
  name: string;
  cron: string;
  sourcePath: string;
  description: string;
};

export type ConfigSignal = {
  type: string;
  path: string;
  summary: string;
};

export type SymbolInfo = {
  name: string;
  kind: string;
  sourcePath: string;
  container: string;
};

export type FileSummary = {
  path: string;
  language: string;
  summary: string;
  tags: string[];
};

export type ProjectIndex = {
  projectId: string;
  stack: string;
  moduleCards: ModuleCard[];
  entryPoints: EntryPointInfo[];
  routes: RouteInfo[];
  jobs: ScheduledJobInfo[];
  configSignals: ConfigSignal[];
  symbolTable: SymbolInfo[];
  fileSummaries: FileSummary[];
  lastIndexedAt: string;
  indexVersion: number;
  excludedPaths: string[];
  analysisWarnings: string[];
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

export type ProjectDetails = {
  connection: ProjectConnection;
  index?: ProjectIndex | null;
  recentJobs: JobStatus[];
};

export type AuthState = {
  copilotCliReady: boolean;
  copilotLoggedIn: boolean;
  copilotEntitled: boolean;
  availableModels: string[];
  loginCommand: string;
  statusMessage: string;
};

export type HealthState = Record<string, string>;

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
