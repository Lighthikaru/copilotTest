import { useEffect, useMemo, useState } from "react";
import { AuthPanel } from "./components/AuthPanel";
import { ChatPanel } from "./components/ChatPanel";
import { ConversationList } from "./components/ConversationList";
import { ProjectConnectionForms } from "./components/ProjectConnectionForms";
import { ProjectList } from "./components/ProjectList";
import { SettingsPanel } from "./components/SettingsPanel";
import { api } from "./lib/api";
import type {
  AppSettings,
  AuthState,
  ConversationDetail,
  ConversationItem,
  HealthState,
  JobStatus,
  ProjectConnection,
} from "./lib/types";
import "./styles/app.css";

export default function App() {
  const [authState, setAuthState] = useState<AuthState | null>(null);
  const [health, setHealth] = useState<HealthState | null>(null);
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [projects, setProjects] = useState<ProjectConnection[]>([]);
  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>();
  const [selectedConversationId, setSelectedConversationId] = useState<string | undefined>();
  const [selectedConversation, setSelectedConversation] = useState<ConversationDetail | null>(null);
  const [selectedModel, setSelectedModel] = useState("gpt-5.4");
  const [activeJob, setActiveJob] = useState<JobStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void refreshShell();
  }, []);

  useEffect(() => {
    if (authState?.availableModels?.length) {
      setSelectedModel((current) =>
        authState.availableModels.includes(current) ? current : authState.availableModels[0],
      );
    }
  }, [authState]);

  useEffect(() => {
    if (!selectedProjectId) {
      setConversations([]);
      setSelectedConversationId(undefined);
      setSelectedConversation(null);
      return;
    }
    void refreshConversations(selectedProjectId);
  }, [selectedProjectId]);

  useEffect(() => {
    if (!selectedProjectId || !selectedConversationId) {
      setSelectedConversation(null);
      return;
    }
    void api
      .getConversation(selectedProjectId, selectedConversationId)
      .then(setSelectedConversation)
      .catch((reason) => setError((reason as Error).message));
  }, [selectedProjectId, selectedConversationId]);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId),
    [projects, selectedProjectId],
  );

  async function refreshShell() {
    try {
      setError(null);
      const [auth, runtimeHealth, savedSettings, projectList] = await Promise.all([
        api.getAuthState(),
        api.getHealth(),
        api.getSettings(),
        api.getProjects(),
      ]);
      setAuthState(auth);
      setHealth(runtimeHealth);
      setSettings(savedSettings);
      setProjects(projectList);
      if (!selectedProjectId && projectList.length) {
        setSelectedProjectId(projectList[0].id);
      }
    } catch (reason) {
      setError((reason as Error).message);
    }
  }

  async function refreshConversations(projectId: string) {
    try {
      const items = await api.getConversations(projectId);
      const visible = items.filter((item) => !item.archived);
      setConversations(visible);
      if (!visible.length) {
        setSelectedConversationId(undefined);
        setSelectedConversation(null);
        return;
      }
      setSelectedConversationId((current) =>
        current && visible.some((item) => item.id === current) ? current : visible[0].id,
      );
    } catch (reason) {
      setError((reason as Error).message);
    }
  }

  async function watchJob(job: JobStatus) {
    setActiveJob(job);
    const eventSource = api.subscribeJob(job.jobId, (nextJob) => {
      setActiveJob(nextJob);
      if (nextJob.state !== "RUNNING") {
        eventSource.close();
        void refreshShell();
      }
    });
  }

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">本機 Web 助手</p>
          <h1>Project Navigator</h1>
          <p>綁定既有本地專案資料夾、建立專案地圖，並透過 Copilot CLI 問答，協助 BA / SA 快速理解程式結構。</p>
        </div>
        {selectedProject ? (
          <div className="hero-card">
            <strong>{selectedProject.displayName}</strong>
            <span>本地工作區</span>
          </div>
        ) : null}
      </section>

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="top-grid">
        <AuthPanel authState={authState} health={health} onRefresh={() => void refreshShell()} />
        <ProjectConnectionForms
          onLocalImport={async (payload) => {
            const job = await api.importLocal(payload);
            await watchJob(job);
          }}
        />
      </div>

      <div className="top-grid">
        <SettingsPanel
          settings={settings}
          onSave={async (nextSettings) => {
            const saved = await api.saveSettings(nextSettings);
            setSettings(saved);
            await refreshShell();
          }}
        />
      </div>

      <div className="content-grid">
        <ProjectList
          projects={projects}
          selectedProject={selectedProject ?? null}
          selectedProjectId={selectedProjectId}
          activeJob={activeJob}
          onSelect={setSelectedProjectId}
          onReindex={async (projectId) => {
            const job = await api.reindex(projectId);
            await watchJob(job);
          }}
        />
        <ConversationList
          conversations={conversations}
          selectedConversationId={selectedConversationId}
          onSelect={setSelectedConversationId}
          onCreate={async () => {
            if (!selectedProject) {
              return;
            }
            const created = await api.createConversation(selectedProject.id);
            await refreshConversations(selectedProject.id);
            setSelectedConversationId(created.id);
          }}
          onRename={async (conversationId) => {
            if (!selectedProject) {
              return;
            }
            const current = conversations.find((item) => item.id === conversationId);
            const title = window.prompt("請輸入新的對話名稱", current?.title ?? "");
            if (!title) {
              return;
            }
            await api.updateConversation(selectedProject.id, conversationId, { title });
            await refreshConversations(selectedProject.id);
          }}
          onArchive={async (conversationId) => {
            if (!selectedProject) {
              return;
            }
            const current = conversations.find((item) => item.id === conversationId);
            await api.updateConversation(selectedProject.id, conversationId, {
              archived: !(current?.archived ?? false),
            });
            await refreshConversations(selectedProject.id);
          }}
        />
        <ChatPanel
          authState={authState}
          selectedModel={selectedModel}
          setSelectedModel={setSelectedModel}
          selectedProject={selectedProject ?? null}
          selectedConversation={selectedConversation}
          onCompressConversation={async () => {
            if (!selectedProject || !selectedConversationId) {
              return;
            }
            const detail = await api.compressConversation(selectedProject.id, selectedConversationId);
            setSelectedConversation(detail);
            await refreshConversations(selectedProject.id);
          }}
          onClearConversation={async () => {
            if (!selectedProject || !selectedConversationId) {
              return;
            }
            const detail = await api.clearConversationMessages(selectedProject.id, selectedConversationId);
            setSelectedConversation(detail);
            await refreshConversations(selectedProject.id);
          }}
          onRestartSession={async () => {
            if (!selectedProject || !selectedConversationId) {
              return;
            }
            const detail = await api.restartConversationSession(selectedProject.id, selectedConversationId);
            setSelectedConversation(detail);
            await refreshConversations(selectedProject.id);
          }}
          onSend={async (question, mode, handlers) => {
            if (!selectedProject) {
              return;
            }

            let conversationId = selectedConversationId;
            if (!conversationId) {
              const created = await api.createConversation(selectedProject.id);
              conversationId = created.id;
              setSelectedConversationId(created.id);
            }

            await api.streamChat(
              {
                projectId: selectedProject.id,
                conversationId,
                question,
                mode,
                selectedModel,
              },
              {
                onMeta: (meta) => {
                  const metaId = String(meta.conversationId ?? conversationId);
                  if (metaId) {
                    setSelectedConversationId(metaId);
                  }
                },
                onChunk: handlers.onChunk,
                onError: handlers.onError,
                onComplete: async (response) => {
                  handlers.onComplete(response);
                  await refreshConversations(selectedProject.id);
                  if (conversationId) {
                    const detail = await api.getConversation(selectedProject.id, conversationId);
                    setSelectedConversation(detail);
                  }
                },
                onWarnings: (warnings) => warnings.length && setError(warnings.join(" | ")),
              },
            );
          }}
        />
      </div>
    </main>
  );
}
