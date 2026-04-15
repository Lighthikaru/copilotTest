import type { ProjectDetails } from "../lib/types";

type Props = {
  details: ProjectDetails | null;
  onReindex: (projectId: string) => Promise<void>;
};

export function ProjectOverview({ details, onReindex }: Props) {
  if (!details) {
    return (
      <section className="panel overview">
        <p className="empty">Select a project to inspect its structure.</p>
      </section>
    );
  }

  const { connection, index } = details;

  return (
    <section className="panel overview">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Overview</p>
          <h2>{connection.displayName}</h2>
        </div>
        <button className="button ghost" onClick={() => void onReindex(connection.id)}>
          Reindex
        </button>
      </div>

      {index ? (
        <>
          <div className="chips">
            <span className="chip ok">{index.stack}</span>
            <span className="chip">Modules: {index.moduleCards.length}</span>
            <span className="chip">Routes: {index.routes.length}</span>
            <span className="chip">Jobs: {index.jobs.length}</span>
            <span className="chip">Excluded: {index.excludedPaths.length}</span>
          </div>

          <div className="overview-grid">
            <section>
              <h3>Module cards</h3>
              <ul className="card-list">
                {index.moduleCards.slice(0, 8).map((module) => (
                  <li key={module.name}>
                    <strong>{module.name}</strong>
                    <span>{module.kind}</span>
                    <p>{module.description}</p>
                  </li>
                ))}
              </ul>
            </section>

            <section>
              <h3>Entry points</h3>
              <ul className="bullet-list">
                {index.entryPoints.map((entry) => (
                  <li key={`${entry.path}-${entry.name}`}>
                    <strong>{entry.name}</strong> · {entry.path}
                  </li>
                ))}
                {!index.entryPoints.length ? <li>No entry points detected.</li> : null}
              </ul>

              <h3>Routes</h3>
              <ul className="bullet-list">
                {index.routes.slice(0, 8).map((route) => (
                  <li key={`${route.sourcePath}-${route.path}-${route.method}`}>
                    <strong>{route.method}</strong> {route.path} · {route.controller}
                  </li>
                ))}
                {!index.routes.length ? <li>No routes detected.</li> : null}
              </ul>
            </section>

            <section>
              <h3>Config signals</h3>
              <ul className="bullet-list">
                {index.configSignals.slice(0, 8).map((config) => (
                  <li key={`${config.path}-${config.type}`}>
                    <strong>{config.type}</strong> · {config.path}
                  </li>
                ))}
              </ul>

              <h3>Excluded from model context</h3>
              <ul className="bullet-list compact">
                {index.excludedPaths.slice(0, 12).map((path) => (
                  <li key={path}>{path}</li>
                ))}
              </ul>
            </section>
          </div>
        </>
      ) : (
        <p className="empty">This project does not have an index yet.</p>
      )}
    </section>
  );
}
