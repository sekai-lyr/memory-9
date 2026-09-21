package com.sekai.sekai_form.agent;

@FunctionalInterface
public interface AgentProgressCallback {
    void onProgress(String phase, String message);
}
