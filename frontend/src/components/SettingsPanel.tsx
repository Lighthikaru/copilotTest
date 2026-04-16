import { useEffect, useState } from "react";
import type { AppSettings } from "../lib/types";

type Props = {
  settings: AppSettings | null;
  onSave: (settings: AppSettings) => Promise<void>;
};

export function SettingsPanel({ settings, onSave }: Props) {
  const [draft, setDraft] = useState<AppSettings>({
    javaPath: "",
    copilotCliPath: "",
    dataRoot: "",
  });
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    setDraft({
      javaPath: settings?.javaPath ?? "",
      copilotCliPath: settings?.copilotCliPath ?? "",
      dataRoot: settings?.dataRoot ?? "",
    });
  }, [settings]);

  const detectedJavaHome = deriveJavaHome(draft.javaPath ?? "");
  const compileCommand = detectedJavaHome
    ? `set "JAVA_HOME=${detectedJavaHome}" && set "PATH=%JAVA_HOME%\\bin;%PATH%" && mvn clean package`
    : `set "JAVA_HOME=C:\\Program Files\\Java\\jdk-21" && set "PATH=%JAVA_HOME%\\bin;%PATH%" && mvn clean package`;

  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">系統設定</p>
          <h2>執行環境</h2>
        </div>
      </div>

      <form
        className="form-grid"
        onSubmit={(event) => {
          event.preventDefault();
          setBusy(true);
          void onSave(draft).finally(() => setBusy(false));
        }}
      >
        <label>
          Java 路徑
          <input
            value={draft.javaPath ?? ""}
            onChange={(event) => setDraft({ ...draft, javaPath: event.target.value })}
            placeholder="C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"
          />
        </label>
        <label>
          Copilot CLI 路徑
          <input
            value={draft.copilotCliPath ?? ""}
            onChange={(event) => setDraft({ ...draft, copilotCliPath: event.target.value })}
            placeholder="C:\\Users\\你的帳號\\AppData\\Roaming\\npm\\copilot.cmd"
          />
        </label>
        <label className="span-2">
          資料目錄
          <input
            value={draft.dataRoot ?? ""}
            onChange={(event) => setDraft({ ...draft, dataRoot: event.target.value })}
            placeholder="C:\\Users\\你的帳號\\.project-navigator"
          />
        </label>
        <p className="status-line span-2">
          Windows 若無法自動找到 Java 或 Copilot CLI，可在這裡手動指定完整路徑。
        </p>
        <label className="span-2">
          Windows CMD 編譯指令
          <textarea
            className="command-box"
            value={compileCommand}
            readOnly
          />
        </label>
        <div className="header-actions span-2">
          <button
            type="button"
            className="button ghost"
            onClick={() => {
              void navigator.clipboard.writeText(compileCommand);
              setCopied(true);
              window.setTimeout(() => setCopied(false), 1500);
            }}
          >
            {copied ? "已複製編譯指令" : "複製編譯指令"}
          </button>
        </div>
        <button className="button primary span-2" disabled={busy}>
          {busy ? "儲存中..." : "儲存設定"}
        </button>
      </form>
    </section>
  );
}

function deriveJavaHome(javaPath: string) {
  const normalized = javaPath.trim();
  if (!normalized) {
    return "";
  }
  const lower = normalized.toLowerCase();
  if (lower.endsWith("\\bin\\java.exe")) {
    return normalized.slice(0, -("\\bin\\java.exe".length));
  }
  if (lower.endsWith("/bin/java")) {
    return normalized.slice(0, -("/bin/java".length));
  }
  return normalized;
}
