import type { AuthState, HealthState } from "../lib/types";

type Props = {
  authState: AuthState | null;
  health: HealthState | null;
  onRefresh: () => void;
};

export function AuthPanel({ authState, health, onRefresh }: Props) {
  return (
    <section className="panel auth-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Copilot</p>
          <h2>CLI onboarding</h2>
        </div>
        <button className="button ghost" onClick={onRefresh}>
          Refresh
        </button>
      </div>

      <p className="status-line">{authState?.statusMessage ?? "Loading Copilot status..."}</p>
      <div className="chips">
        <span className={`chip ${authState?.copilotCliReady ? "ok" : "warn"}`}>
          CLI: {authState?.copilotCliReady ? "ready" : "missing"}
        </span>
        <span className={`chip ${authState?.copilotLoggedIn ? "ok" : "warn"}`}>
          Login: {authState?.copilotLoggedIn ? "verified" : "required"}
        </span>
        <span className={`chip ${authState?.copilotEntitled ? "ok" : "warn"}`}>
          Chat: {authState?.copilotEntitled ? "enabled" : "disabled"}
        </span>
      </div>

      <div className="meta-grid">
        <div>
          <h3>How to enable chat</h3>
          <ol className="plain-steps">
            <li>Open a terminal on this same machine.</li>
            <li>Run <code>{authState?.loginCommand ?? "copilot login"}</code>.</li>
            <li>Finish the browser authorization.</li>
            <li>Come back here and click Refresh.</li>
          </ol>
        </div>
        <div>
          <h3>Available models</h3>
          <ul>
            {(authState?.availableModels ?? []).map((model) => (
              <li key={model}>{model}</li>
            ))}
            {!authState?.availableModels?.length ? <li>Model list will appear after CLI login succeeds.</li> : null}
          </ul>
        </div>
      </div>

      <div className="meta-grid">
        <div>
          <h3>Runtime health</h3>
          <ul>
            {health
              ? Object.entries(health).map(([key, value]) => (
                  <li key={key}>
                    <strong>{key}</strong>: {value}
                  </li>
                ))
              : null}
          </ul>
        </div>
        <div>
          <h3>MVP scope</h3>
          <ul>
            <li>Bind an existing local project folder.</li>
            <li>Build a project map locally.</li>
            <li>Use Copilot CLI as the chat identity.</li>
          </ul>
        </div>
      </div>
    </section>
  );
}
