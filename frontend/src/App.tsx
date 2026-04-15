import { useEffect, useMemo, useState } from "react";
import { AuthPanel } from "./components/AuthPanel";
import { ChatPanel } from "./components/ChatPanel";
import { ProjectConnectionForms } from "./components/ProjectConnectionForms";
import { ProjectList } from "./components/ProjectList";
import { api } from "./lib/api";
import type {
  AuthState,
  HealthState,
  JobStatus,
  ProjectConnection,
} from "./lib/types";
import "./styles/app.css";

export default function App() {
  const [authState, setAuthState] = useState<AuthState | null>(null);
  const [health, setHealth] = useState<HealthState | null>(null);
  const [projects, setProjects] = useState<ProjectConnection[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>();
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

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId),
    [projects, selectedProjectId],
  );

  async function refreshShell() {
    try {
      setError(null);
      const [auth, runtimeHealth, projectList] = await Promise.all([
        api.getAuthState(),
        api.getHealth(),
        api.getProjects(),
      ]);
      setAuthState(auth);
      setHealth(runtimeHealth);
      setProjects(projectList);
      if (!selectedProjectId && projectList.length) {
        setSelectedProjectId(projectList[0].id);
      }
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
          <p className="eyebrow">Local Tomcat Web App</p>
          <h1>Project Navigator</h1>
          <p>
            Bind an existing local project folder, build a project map, then use Copilot CLI-backed
            chat to help BA and SA understand the codebase.
          </p>
        </div>
        {selectedProject ? (
          <div className="hero-card">
            <strong>{selectedProject.displayName}</strong>
            <span>Local workspace</span>
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
        <ChatPanel
          authState={authState}
          selectedModel={selectedModel}
          setSelectedModel={setSelectedModel}
          selectedProject={selectedProject ?? null}
          onSend={async (question, mode, handlers) => {
            if (!selectedProject) {
              return;
            }
            await api.streamChat(
              {
                projectId: selectedProject.id,
                question,
                mode,
                selectedModel,
              },
              {
                onChunk: handlers.onChunk,
                onError: handlers.onError,
                onComplete: handlers.onComplete,
                onWarnings: (warnings) => warnings.length && setError(warnings.join(" | ")),
              },
            );
          }}
        />
      </div>
    </main>
  );
}
