package com.sekai.sekai_form.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolResult<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final boolean retryable;
    private final Map<String, String> artifacts;

    private ToolResult(boolean success, String message, T data, boolean retryable, Map<String, String> artifacts) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.data = data;
        this.retryable = retryable;
        this.artifacts = Collections.unmodifiableMap(new LinkedHashMap<>(artifacts == null ? Map.of() : artifacts));
    }

    public static <T> ToolResult<T> success(T data) { return new ToolResult<>(true, "", data, false, Map.of()); }
    public static <T> ToolResult<T> success(String message, T data) { return new ToolResult<>(true, message, data, false, Map.of()); }
    public static <T> ToolResult<T> successWithArtifact(String message, T data, Map<String, String> artifacts) {
        return new ToolResult<>(true, message, data, false, artifacts);
    }
    public static <T> ToolResult<T> failure(String message, boolean retryable) {
        return new ToolResult<>(false, message, null, retryable, Map.of());
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public boolean isRetryable() { return retryable; }
    public Map<String, String> getArtifacts() { return artifacts; }
}
