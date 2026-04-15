import { startTransition, useEffect, useMemo, useRef, useState } from "react";
import type { AuthState, ChatResponse, ProjectConnection, SourceRef } from "../lib/types";

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

export function ChatPanel({ authState, selectedModel, setSelectedModel, selectedProject, onSend }: Props) {
  const [question, setQuestion] = useState("");
  const [mode, setMode] = useState<"structure" | "impact">("structure");
  const [messages, setMessages] = useState<Message[]>([]);
  const [pendingAnswer, setPendingAnswer] = useState("");
  const [busy, setBusy] = useState(false);
  const chunkBufferRef = useRef("");
  const flushTimerRef = useRef<number | null>(null);
  const renderedPendingRef = useRef("");

  const disabledReason = useMemo(() => {
    if (!selectedProject) {
      return "Select a project first.";
    }
    if (!authState?.copilotEntitled) {
      return authState?.statusMessage ?? "Copilot is not ready yet.";
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

    const askedQuestion = question;
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
      handleStreamError((reason as Error).message || "Chat request failed.");
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

  const showThinkingState = busy && !pendingAnswer;

  return (
    <section className="panel chat-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Chat</p>
          <h2>Ask about the project</h2>
          {selectedProject ? <p className="status-line">Current project: {selectedProject.displayName}</p> : null}
        </div>
        <div className="inline-controls">
          <select value={mode} onChange={(event) => setMode(event.target.value as "structure" | "impact")}>
            <option value="structure">Structure</option>
            <option value="impact">Impact</option>
          </select>
          <select value={selectedModel} onChange={(event) => setSelectedModel(event.target.value)}>
            {(authState?.availableModels ?? []).map((model) => (
              <option key={model} value={model}>
                {model}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="chat-log">
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
            <p>Thinking...</p>
          </article>
        ) : null}
        {!messages.length && !pendingAnswer && !showThinkingState ? (
          <div className="empty">
            Ask "這個專案的登入流程在哪裡？" or "新增欄位 customerLevel 會影響哪些地方？"
          </div>
        ) : null}
      </div>

      <div className="composer">
        <textarea
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          placeholder={disabledReason ?? "Ask a question about structure or impact."}
          disabled={Boolean(disabledReason) || busy}
        />
        <button className="button primary" onClick={() => void handleSubmit()} disabled={Boolean(disabledReason) || busy}>
          {busy ? "Thinking..." : "Send"}
        </button>
      </div>
      {disabledReason ? <p className="status-line">{disabledReason}</p> : null}
    </section>
  );
}
