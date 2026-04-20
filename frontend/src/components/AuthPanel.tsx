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
          <h2>CLI 啟用流程</h2>
          <p className="panel-subtitle">確認 CLI、登入與 entitlement 是否就緒，避免聊天流程中斷。</p>
        </div>
        <button className="button ghost" onClick={onRefresh}>
          重新整理
        </button>
      </div>

      <p className="status-line">{authState?.statusMessage ?? "正在讀取 Copilot 狀態..."}</p>
      <div className="chips">
        <span className={`chip ${authState?.copilotCliReady ? "ok" : "warn"}`}>
          CLI：{authState?.copilotCliReady ? "可用" : "未就緒"}
        </span>
        <span className={`chip ${authState?.copilotLoggedIn ? "ok" : "warn"}`}>
          登入：{authState?.copilotLoggedIn ? "已驗證" : "需要登入"}
        </span>
        <span className={`chip ${authState?.copilotEntitled ? "ok" : "warn"}`}>
          聊天：{authState?.copilotEntitled ? "可使用" : "不可使用"}
        </span>
        <span className={`chip ${authState?.javaReady ? "ok" : "warn"}`}>
          Java：{authState?.javaReady ? "可用" : "未就緒"}
        </span>
      </div>

      <div className="meta-grid">
        <div>
          <h3>如何啟用聊天</h3>
          <ol className="plain-steps">
            <li>在同一台機器的終端機執行 <code>npm install -g @github/copilot</code></li>
            <li>執行 <code>{authState?.loginCommand ?? "copilot login"}</code></li>
            <li>完成瀏覽器授權</li>
            <li>回到這裡按「重新整理」</li>
          </ol>
        </div>
        <div>
          <h3>可用模型</h3>
          <ul>
            {(authState?.availableModels ?? []).map((model) => (
              <li key={model}>{model}</li>
            ))}
            {!authState?.availableModels?.length ? <li>CLI 登入與 entitlement 成功後，模型清單會出現在這裡。</li> : null}
          </ul>
        </div>
      </div>

      <div className="meta-grid">
        <div>
          <h3>執行環境健康檢查</h3>
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
          <h3>目前版本範圍</h3>
          <ul>
            <li>綁定既有本地專案資料夾</li>
            <li>在本機建立專案地圖</li>
            <li>透過 Copilot CLI 進行問答</li>
          </ul>
        </div>
      </div>
    </section>
  );
}
