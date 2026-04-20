import type {
  AppSettings,
  AuthState,
  ChatRequest,
  ChatResponse,
  ConversationDetail,
  ConversationItem,
  HealthState,
  JobStatus,
  LocalProjectRequest,
  ProjectConnection,
} from "./types";

async function request<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `${response.status} ${response.statusText}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  getHealth: () => request<HealthState>("/api/health"),
  getAuthState: () => request<AuthState>("/api/auth/state"),
  getSettings: () => request<AppSettings>("/api/settings"),
  saveSettings: (payload: AppSettings) =>
    request<AppSettings>("/api/settings", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  getProjects: () => request<ProjectConnection[]>("/api/projects"),
  importLocal: (payload: LocalProjectRequest) =>
    request<JobStatus>("/api/projects/local", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  reindex: (projectId: string) =>
    request<JobStatus>(`/api/projects/${projectId}/reindex`, {
      method: "POST",
    }),
  deleteProject: (projectId: string) =>
    request<void>(`/api/projects/${projectId}`, {
      method: "DELETE",
    }),
  getJob: (jobId: string) => request<JobStatus>(`/api/jobs/${jobId}`),
  getConversations: (projectId: string) => request<ConversationItem[]>(`/api/projects/${projectId}/conversations`),
  createConversation: (projectId: string, title?: string) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations`, {
      method: "POST",
      body: JSON.stringify(title ? { title } : {}),
    }),
  getConversation: (projectId: string, conversationId: string) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations/${conversationId}`),
  updateConversation: (projectId: string, conversationId: string, payload: { title?: string; archived?: boolean }) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations/${conversationId}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),
  compressConversation: (projectId: string, conversationId: string) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations/${conversationId}/compress`, {
      method: "POST",
    }),
  restartConversationSession: (projectId: string, conversationId: string) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations/${conversationId}/restart-session`, {
      method: "POST",
    }),
  clearConversationMessages: (projectId: string, conversationId: string) =>
    request<ConversationDetail>(`/api/projects/${projectId}/conversations/${conversationId}/messages`, {
      method: "DELETE",
    }),
  deleteConversation: (projectId: string, conversationId: string) =>
    request<void>(`/api/projects/${projectId}/conversations/${conversationId}`, {
      method: "DELETE",
    }),
  subscribeJob(jobId: string, onMessage: (job: JobStatus) => void) {
    const eventSource = new EventSource(`/api/jobs/${jobId}/stream`);
    eventSource.addEventListener("job", (event) => {
      const message = event as MessageEvent<string>;
      onMessage(JSON.parse(message.data) as JobStatus);
    });
    return eventSource;
  },
  async streamChat(
    payload: ChatRequest,
    callbacks: {
      onMeta?: (meta: Record<string, unknown>) => void;
      onChunk?: (chunk: string) => void;
      onError?: (message: string) => void;
      onWarnings?: (warnings: string[]) => void;
      onComplete?: (response: ChatResponse) => void;
    },
  ) {
    const response = await fetch("/api/chat/stream", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok || !response.body) {
      throw new Error(await response.text());
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let streamFailed = false;

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });

      let boundary = findSseBoundary(buffer);
      while (boundary) {
        const rawEvent = buffer.slice(0, boundary.index);
        buffer = buffer.slice(boundary.index + boundary.length);
        if (parseSseEvent(rawEvent, callbacks) === "error") {
          streamFailed = true;
          await reader.cancel();
          break;
        }
        boundary = findSseBoundary(buffer);
      }

      if (streamFailed) {
        break;
      }
    }

    if (!streamFailed && buffer.trim()) {
      parseSseEvent(buffer, callbacks);
    }
  },
};

function parseSseEvent(
  rawEvent: string,
  callbacks: {
    onMeta?: (meta: Record<string, unknown>) => void;
    onChunk?: (chunk: string) => void;
    onError?: (message: string) => void;
    onWarnings?: (warnings: string[]) => void;
    onComplete?: (response: ChatResponse) => void;
  },
): "error" | "handled" | "ignored" {
  const lines = rawEvent.split(/\r?\n/);
  const eventLine = lines.find((line) => line.startsWith("event:"));
  const dataLines = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => {
      const value = line.slice(5);
      return value.startsWith(" ") ? value.slice(1) : value;
    });
  const eventName = eventLine?.replace("event:", "").trim();
  const data = dataLines.join("\n");

  if (!eventName) {
    return "ignored";
  }

  if (eventName === "chunk") {
    callbacks.onChunk?.(data);
    return "handled";
  }

  if (eventName === "error") {
    callbacks.onError?.(data || "聊天請求失敗。");
    return "error";
  }

  if (eventName === "warnings") {
    callbacks.onWarnings?.(JSON.parse(data) as string[]);
    return "handled";
  }

  if (eventName === "meta") {
    callbacks.onMeta?.(JSON.parse(data) as Record<string, unknown>);
    return "handled";
  }

  if (eventName === "complete") {
    callbacks.onComplete?.(JSON.parse(data) as ChatResponse);
    return "handled";
  }

  return "ignored";
}

function findSseBoundary(buffer: string): { index: number; length: number } | null {
  const match = /\r?\n\r?\n/.exec(buffer);
  if (!match || match.index < 0) {
    return null;
  }
  return {
    index: match.index,
    length: match[0].length,
  };
}
