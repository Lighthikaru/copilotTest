import { useMemo, useState } from "react";
import type { LocalProjectRequest } from "../lib/types";

type Props = {
  onLocalImport: (payload: LocalProjectRequest) => Promise<void>;
};

export function ProjectConnectionForms({ onLocalImport }: Props) {
  const [local, setLocal] = useState<LocalProjectRequest>({
    displayName: "",
    localPath: "",
  });

  const canSubmitLocal = useMemo(
    () => local.displayName.trim() && local.localPath.trim(),
    [local],
  );

  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">綁定專案</p>
          <h2>綁定本地專案資料夾</h2>
        </div>
      </div>

      <form
        className="form-grid"
        onSubmit={(event) => {
          event.preventDefault();
          void onLocalImport(local);
        }}
      >
        <label>
          顯示名稱
          <input
            value={local.displayName}
            onChange={(event) => setLocal({ ...local, displayName: event.target.value })}
            placeholder="customer-core"
          />
        </label>
        <label className="span-2">
          本地資料夾路徑
          <input
            value={local.localPath}
            onChange={(event) => setLocal({ ...local, localPath: event.target.value })}
            placeholder="C:\\workspace\\customer-core"
          />
        </label>
        <p className="status-line span-2">
          這個版本只支援讀取既有本地專案資料夾，不提供 GitLab clone 流程。
        </p>
        <button className="button primary span-2" disabled={!canSubmitLocal}>
          綁定資料夾並建立索引
        </button>
      </form>
    </section>
  );
}
