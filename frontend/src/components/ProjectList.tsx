import type { JobStatus, ProjectConnection } from "../lib/types";

type Props = {
  projects: ProjectConnection[];
  selectedProject: ProjectConnection | null;
  selectedProjectId?: string;
  activeJob?: JobStatus | null;
  onSelect: (projectId: string) => void;
  onReindex: (projectId: string) => Promise<void>;
};

export function ProjectList({
  projects,
  selectedProject,
  selectedProjectId,
  activeJob,
  onSelect,
  onReindex,
}: Props) {
  return (
    <section className="panel project-list">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Library</p>
          <h2>Projects</h2>
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
              Reindex
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
            <div>
              <strong>{project.displayName}</strong>
              <p>{project.localPath}</p>
            </div>
            <span>{new Date(project.updatedAt).toLocaleString()}</span>
          </button>
        ))}
        {!projects.length ? <p className="empty">No projects imported yet.</p> : null}
      </div>
    </section>
  );
}
