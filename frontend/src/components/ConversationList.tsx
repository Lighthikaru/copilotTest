import type { ConversationItem } from "../lib/types";

type Props = {
  conversations: ConversationItem[];
  selectedConversationId?: string;
  onSelect: (conversationId: string) => void;
  onCreate: () => Promise<void>;
  onRename: (conversationId: string) => Promise<void>;
  onDelete: (conversationId: string) => Promise<void>;
};

export function ConversationList({
  conversations,
  selectedConversationId,
  onSelect,
  onCreate,
  onRename,
  onDelete,
}: Props) {
  return (
    <section className="panel conversation-list">
      <div className="panel-header">
        <div>
          <p className="eyebrow">對話</p>
          <h2>聊天紀錄</h2>
          <p className="panel-subtitle">保留結構理解與影響分析的上下文，快速切回最近工作。</p>
        </div>
        <button className="button primary" onClick={() => void onCreate()}>
          新對話
        </button>
      </div>

      <div className="stack-list">
        {conversations.map((conversation) => (
          <article
            key={conversation.id}
            className={`conversation-card ${conversation.id === selectedConversationId ? "selected" : ""}`}
          >
            <button className="conversation-select" onClick={() => onSelect(conversation.id)}>
              <strong>{conversation.title}</strong>
              <p>
                {conversation.lastMode === "impact" ? "影響分析" : "結構理解"} · {conversation.messageCount} 則訊息
              </p>
            </button>
            <div className="conversation-actions">
              <button className="button ghost" onClick={() => void onRename(conversation.id)}>
                重新命名
              </button>
              <button className="button ghost danger" onClick={() => void onDelete(conversation.id)}>
                刪除對話
              </button>
            </div>
          </article>
        ))}
        {!conversations.length ? <p className="empty">這個專案還沒有聊天紀錄，先建立一個新對話吧。</p> : null}
      </div>
    </section>
  );
}
