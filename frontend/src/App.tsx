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
      setConversations(items);
      if (!items.length) {
        setSelectedConversationId(undefined);
        setSelectedConversation(null);
        return;
      }
      setSelectedConversationId((current) =>
        current && items.some((item) => item.id === current) ? current : items[0].id,
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
        <div className="hero-copy">
          <p className="eyebrow">本機 Web 助手</p>
          <h1>Project Navigator</h1>
          <p>把專案索引、對話管理與 Copilot 問答整合在同一個深色工作台，讓理解架構和追查影響範圍更順手。</p>
        </div>
        <div className="hero-status">
          <div className="hero-stat">
            <span>專案數</span>
            <strong>{projects.length}</strong>
          </div>
          <div className="hero-stat hero-stat-accent">
            <span>目前工作區</span>
            <strong>{selectedProject?.displayName ?? "尚未選擇"}</strong>
          </div>
          <div className="hero-stat">
            <span>對話數</span>
            <strong>{conversations.length}</strong>
          </div>
        </div>
      </section>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="overview-grid">
        <AuthPanel authState={authState} health={health} onRefresh={() => void refreshShell()} />
        <ProjectConnectionForms
          onLocalImport={async (payload) => {
            const job = await api.importLocal(payload);
            await watchJob(job);
          }}
        />
        <SettingsPanel
          settings={settings}
          onSave={async (nextSettings) => {
            const saved = await api.saveSettings(nextSettings);
            setSettings(saved);
            await refreshShell();
          }}
        />
      </section>

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
          onDelete={async (projectId) => {
            const project = projects.find((item) => item.id === projectId);
            const confirmed = window.confirm(
              `確定要刪除專案「${project?.displayName ?? projectId}」嗎？這只會刪除 Project Navigator 的工具資料，不會刪除原始碼資料夾。`,
            );
            if (!confirmed) {
              return;
            }
            await api.deleteProject(projectId);
            const nextProjects = projects.filter((item) => item.id !== projectId);
            setProjects(nextProjects);
            if (selectedProjectId === projectId) {
              setSelectedProjectId(nextProjects[0]?.id);
              setSelectedConversationId(undefined);
              setSelectedConversation(null);
            }
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
          onDelete={async (conversationId) => {
            if (!selectedProject) {
              return;
            }
            const current = conversations.find((item) => item.id === conversationId);
            const confirmed = window.confirm(
              `確定要永久刪除對話「${current?.title ?? conversationId}」嗎？此動作會刪除聊天紀錄、上下文與 session 資料，且無法復原。`,
            );
            if (!confirmed) {
              return;
            }
            await api.deleteConversation(selectedProject.id, conversationId);
            await refreshConversations(selectedProject.id);
            if (selectedConversationId === conversationId) {
              const remaining = conversations.filter((item) => item.id !== conversationId);
              setSelectedConversationId(remaining[0]?.id);
              if (!remaining.length) {
                setSelectedConversation(null);
              }
            }
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
                    conversationId = metaId;
                    setSelectedConversationId(metaId);
                  }
                },
                onChunk: handlers.onChunk,
                onError: handlers.onError,
                onComplete: async (response) => {
                  handlers.onComplete(response);
                  if (conversationId) {
                    const [detail] = await Promise.all([
                      api.getConversation(selectedProject.id, conversationId),
                      refreshConversations(selectedProject.id),
                    ]);
                    setSelectedConversation(detail);
                  } else {
                    await refreshConversations(selectedProject.id);
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
