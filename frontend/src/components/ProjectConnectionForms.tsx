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
          <p className="eyebrow">Connect</p>
          <h2>Bind local project folder</h2>
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
          Display name
          <input
            value={local.displayName}
            onChange={(event) => setLocal({ ...local, displayName: event.target.value })}
            placeholder="customer-core"
          />
        </label>
        <label className="span-2">
          Existing local path
          <input
            value={local.localPath}
            onChange={(event) => setLocal({ ...local, localPath: event.target.value })}
            placeholder="C:\\workspace\\customer-core"
          />
        </label>
        <p className="status-line span-2">
          MVP only reads an existing local project folder. GitLab clone flow is intentionally disabled for this version.
        </p>
        <button className="button primary span-2" disabled={!canSubmitLocal}>
          Bind folder and index
        </button>
      </form>
    </section>
  );
}
