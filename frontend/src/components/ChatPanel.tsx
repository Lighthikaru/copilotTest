import { startTransition, useEffect, useMemo, useRef, useState } from "react";
import type { AuthState, ChatResponse, ConversationDetail, ProjectConnection, SourceRef } from "../lib/types";

type Message = {
  role: "user" | "assistant";
  text: string;
  sources?: SourceRef[];
  variant?: "error";
};

type Props = {
  authState: AuthState | null;
  selectedModel: string;
  setSelectedModel: (model: string) => void;
  selectedProject: ProjectConnection | null;
  selectedConversation: ConversationDetail | null;
  onCompressConversation: () => Promise<void>;
  onClearConversation: () => Promise<void>;
  onRestartSession: () => Promise<void>;
  onSend: (
    question: string,
    mode: "structure" | "impact",
    handlers: {
      onChunk: (chunk: string) => void;
      onError: (message: string) => void;
      onComplete: (response: ChatResponse) => void;
    },
  ) => Promise<void>;
};

const STREAM_FLUSH_MS = 75;

export function ChatPanel({
  authState,
  selectedModel,
  setSelectedModel,
  selectedProject,
  selectedConversation,
  onCompressConversation,
  onClearConversation,
  onRestartSession,
  onSend,
}: Props) {
  const [question, setQuestion] = useState("");
  const [mode, setMode] = useState<"structure" | "impact">("structure");
  const [messages, setMessages] = useState<Message[]>([]);
  const [pendingAnswer, setPendingAnswer] = useState("");
  const [busy, setBusy] = useState(false);
  const chunkBufferRef = useRef("");
  const flushTimerRef = useRef<number | null>(null);
  const renderedPendingRef = useRef("");
  const chatLogRef = useRef<HTMLDivElement | null>(null);
  const shouldStickToBottomRef = useRef(true);

  const disabledReason = useMemo(() => {
    if (!selectedProject) {
      return "請先選擇專案。";
    }
    if (!authState?.copilotEntitled) {
      return authState?.statusMessage ?? "Copilot 尚未就緒。";
    }
    return null;
  }, [authState, selectedProject]);

  useEffect(() => {
    return () => {
      if (flushTimerRef.current !== null) {
        window.clearTimeout(flushTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    renderedPendingRef.current = pendingAnswer;
  }, [pendingAnswer]);

  useEffect(() => {
    const log = chatLogRef.current;
    if (!log) {
      return;
    }
    if (!shouldStickToBottomRef.current) {
      return;
    }
    log.scrollTop = log.scrollHeight;
  }, [messages, pendingAnswer, busy, selectedConversation?.id]);

  useEffect(() => {
    setMessages(
      (selectedConversation?.messages ?? []).map((message) => ({
        role: message.role,
        text: message.text,
        sources: message.sources,
      })),
    );
    setPendingAnswer("");
    chunkBufferRef.current = "";
    renderedPendingRef.current = "";
    if (selectedConversation?.lastMode === "impact" || selectedConversation?.lastMode === "structure") {
      setMode(selectedConversation.lastMode);
    }
    if (selectedConversation?.lastModel) {
      setSelectedModel(selectedConversation.lastModel);
    }
  }, [selectedConversation, setSelectedModel]);

  function flushBufferedChunks() {
    if (flushTimerRef.current !== null) {
      window.clearTimeout(flushTimerRef.current);
      flushTimerRef.current = null;
    }

    if (!chunkBufferRef.current) {
      return;
    }

    const nextChunk = chunkBufferRef.current;
    chunkBufferRef.current = "";
    startTransition(() => {
      setPendingAnswer((current) => current + nextChunk);
    });
  }

  function scheduleChunkFlush() {
    if (flushTimerRef.current !== null) {
      return;
    }

    flushTimerRef.current = window.setTimeout(() => {
      flushTimerRef.current = null;
      flushBufferedChunks();
    }, STREAM_FLUSH_MS);
  }

  function appendStreamChunk(chunk: string) {
    chunkBufferRef.current += chunk;
    scheduleChunkFlush();
  }

  function pushAssistantMessage(text: string, sources?: SourceRef[], variant?: "error") {
    if (!text.trim()) {
      return;
    }
    setMessages((current) => [...current, { role: "assistant", text, sources, variant }]);
  }

  function handleStreamError(message: string) {
    flushBufferedChunks();
    const partialAnswer = renderedPendingRef.current;
    renderedPendingRef.current = "";
    setPendingAnswer("");

    if (partialAnswer.trim()) {
      pushAssistantMessage(partialAnswer);
    }

    pushAssistantMessage(message, undefined, "error");
  }

  async function handleSubmit() {
    if (!question.trim() || disabledReason || busy) {
      return;
    }

    const askedQuestion = question.trim();
    setQuestion("");
    setBusy(true);
    setPendingAnswer("");
    setMessages((current) => [...current, { role: "user", text: askedQuestion }]);
    chunkBufferRef.current = "";

    try {
      await onSend(askedQuestion, mode, {
        onChunk: appendStreamChunk,
        onError: handleStreamError,
        onComplete: (response) => {
          flushBufferedChunks();
          renderedPendingRef.current = "";
          setPendingAnswer("");
          pushAssistantMessage(response.answer, response.sources);
        },
      });
    } catch (reason) {
      handleStreamError((reason as Error).message || "聊天請求失敗。");
    } finally {
      flushBufferedChunks();
      if (flushTimerRef.current !== null) {
        window.clearTimeout(flushTimerRef.current);
        flushTimerRef.current = null;
      }
      chunkBufferRef.current = "";
      setBusy(false);
    }
  }

  function handleComposerKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) {
      return;
    }
    event.preventDefault();
    void handleSubmit();
  }

  function handleChatLogScroll(event: React.UIEvent<HTMLDivElement>) {
    const log = event.currentTarget;
    const distanceFromBottom = log.scrollHeight - log.scrollTop - log.clientHeight;
    shouldStickToBottomRef.current = distanceFromBottom < 48;
  }

  const showThinkingState = busy && !pendingAnswer;

  return (
    <section className="panel chat-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">聊天</p>
          <h2>專案問答</h2>
          {selectedProject ? <p className="status-line">目前專案：{selectedProject.displayName}</p> : null}
          {selectedConversation ? (
            <p className="status-line">
              目前對話：{selectedConversation.title}
              {selectedConversation.summarized ? " · 已整理上文" : ""}
            </p>
          ) : null}
        </div>
        <div className="inline-controls">
          <select value={mode} onChange={(event) => setMode(event.target.value as "structure" | "impact")}>
            <option value="structure">結構理解</option>
            <option value="impact">影響分析</option>
          </select>
          <select value={selectedModel} onChange={(event) => setSelectedModel(event.target.value)}>
            {(authState?.availableModels ?? []).map((model) => (
              <option key={model} value={model}>
                {model}
              </option>
            ))}
          </select>
          <button className="button ghost" onClick={() => void onCompressConversation()} disabled={!selectedConversation || busy}>
            整理上文
          </button>
          <button className="button ghost" onClick={() => void onRestartSession()} disabled={!selectedConversation || busy}>
            重啟 Session
          </button>
          <button className="button ghost" onClick={() => void onClearConversation()} disabled={!selectedConversation || busy}>
            清空對話
          </button>
        </div>
      </div>

      <div ref={chatLogRef} className="chat-log" aria-live="polite" onScroll={handleChatLogScroll}>
        {messages.map((message, index) => (
          <article
            key={`${message.role}-${index}`}
            className={`bubble ${message.role}${message.variant ? ` ${message.variant}` : ""}`}
          >
            <p>{message.text}</p>
            {message.sources?.length ? (
              <div className="source-list">
                {message.sources.map((source) => (
                  <details key={`${index}-${source.path}`}>
                    <summary>{source.path}</summary>
                    <pre>{source.excerpt}</pre>
                  </details>
                ))}
              </div>
            ) : null}
          </article>
        ))}
        {pendingAnswer ? (
          <article className="bubble assistant pending">
            <p>{pendingAnswer}</p>
          </article>
        ) : null}
        {showThinkingState ? (
          <article className="bubble assistant pending">
            <p>思考中...</p>
          </article>
        ) : null}
        {!messages.length && !pendingAnswer && !showThinkingState ? (
          <div className="empty">可以試著問：「這個專案的登入流程在哪裡？」或「新增欄位 customerLevel 會影響哪些地方？」</div>
        ) : null}
      </div>

      <div className="composer">
        <textarea
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          onKeyDown={handleComposerKeyDown}
          placeholder={disabledReason ?? "輸入你想問的內容，例如結構理解或影響分析。"}
          disabled={Boolean(disabledReason) || busy}
          rows={4}
        />
        <button className="button primary" onClick={() => void handleSubmit()} disabled={Boolean(disabledReason) || busy}>
          {busy ? "思考中..." : "送出"}
        </button>
      </div>
      <div className="composer-meta">
        <span>{busy ? "系統正在產生回覆，畫面會即時更新。" : "Enter 送出，Shift+Enter 換行。"}</span>
      </div>
      {disabledReason ? <p className="status-line">{disabledReason}</p> : null}
    </section>
  );
}
