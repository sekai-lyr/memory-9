package com.sekai.sekai_form.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentResult {
    private final boolean success;
    private final String reply;
    private final String motion;
    private final String imageUrl;
    private final String audioUrl;
    private final String failureReason;
    private final String traceId;
    private final List<ToolCallResult> toolCalls;

    private AgentResult(boolean success, String reply, String motion, String imageUrl, String audioUrl,
                        String failureReason, String traceId, List<ToolCallResult> toolCalls) {
        this.success = success;
        this.reply = reply == null ? "" : reply;
        this.motion = motion == null ? "Idle" : motion;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.failureReason = failureReason;
        this.traceId = traceId;
        this.toolCalls = Collections.unmodifiableList(new ArrayList<>(toolCalls == null ? List.of() : toolCalls));
    }

    public static AgentResult success(String reply, String traceId, List<ToolCallResult> calls) {
        return new AgentResult(true, reply, motionFor(reply), null, null, null, traceId, calls);
    }

    public static AgentResult successWithMedia(String reply, String imageUrl, String audioUrl,
                                               String traceId, List<ToolCallResult> calls) {
        return new AgentResult(true, reply, motionFor(reply), imageUrl, audioUrl, null, traceId, calls);
    }

    public static AgentResult failure(String message, String traceId, List<ToolCallResult> calls) {
        return new AgentResult(false, message, "Idle", null, null, message, traceId, calls);
    }

    private static String motionFor(String reply) {
        if (reply == null) return "Idle";
        return reply.contains("?") || reply.contains("!") || reply.contains("！") || reply.contains("？") ? "Tap" : "Idle";
    }

    public boolean isSuccess() { return success; }
    public String getReply() { return reply; }
    public String getMotion() { return motion; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
    public String getFailureReason() { return failureReason; }
    public String getTraceId() { return traceId; }
    public List<ToolCallResult> getToolCalls() { return toolCalls; }
}
