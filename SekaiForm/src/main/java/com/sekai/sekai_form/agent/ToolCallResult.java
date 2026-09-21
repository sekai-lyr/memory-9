package com.sekai.sekai_form.agent;

public final class ToolCallResult {
    private final String toolName;
    private final boolean success;
    private final int attempt;
    private final long durationMs;
    private final String output;

    public ToolCallResult(String toolName, boolean success, int attempt, long durationMs, String output) {
        this.toolName = toolName;
        this.success = success;
        this.attempt = attempt;
        this.durationMs = durationMs;
        this.output = output == null ? "" : output;
    }

    public String getToolName() { return toolName; }
    public boolean isSuccess() { return success; }
    public int getAttempt() { return attempt; }
    public long getDurationMs() { return durationMs; }
    public String getOutput() { return output; }
}
