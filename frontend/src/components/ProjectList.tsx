import type { JobStatus, ProjectConnection } from "../lib/types";

type Props = {
  projects: ProjectConnection[];
  selectedProject: ProjectConnection | null;
  selectedProjectId?: string;
  activeJob?: JobStatus | null;
  onSelect: (projectId: string) => void;
  onReindex: (projectId: string) => Promise<void>;
  onDelete: (projectId: string) => Promise<void>;
};

export function ProjectList({
  projects,
  selectedProject,
  selectedProjectId,
  activeJob,
  onSelect,
  onReindex,
  onDelete,
}: Props) {
  return (
    <section className="panel project-list">
      <div className="panel-header">
        <div>
          <p className="eyebrow">專案庫</p>
          <h2>專案清單</h2>
          <p className="panel-subtitle">選擇工作區後，右側聊天與對話會同步切換。</p>
        </div>
        <div className="header-actions">
          {activeJob ? (
            <div className="job-badge">
              <strong>{activeJob.type}</strong>
              <span>
                {activeJob.state} · {activeJob.progress}%
              </span>
            </div>
          ) : null}
          {selectedProject ? (
            <button
              className="button ghost"
              onClick={() => void onReindex(selectedProject.id)}
              disabled={activeJob?.state === "RUNNING"}
            >
              重新索引
            </button>
          ) : null}
          {selectedProject ? (
            <button
              className="button ghost danger"
              onClick={() => void onDelete(selectedProject.id)}
              disabled={activeJob?.state === "RUNNING"}
            >
              刪除專案
            </button>
          ) : null}
        </div>
      </div>

      <div className="stack-list">
        {projects.map((project) => (
          <button
            key={project.id}
            className={`project-row ${project.id === selectedProjectId ? "selected" : ""}`}
            onClick={() => onSelect(project.id)}
          >
            <div className="project-row-copy">
              <strong>{project.displayName}</strong>
              <p>{project.localPath}</p>
            </div>
            <span className="project-row-date">{new Date(project.updatedAt).toLocaleString()}</span>
          </button>
        ))}
        {!projects.length ? <p className="empty">目前還沒有匯入任何專案。</p> : null}
      </div>
    </section>
  );
}
