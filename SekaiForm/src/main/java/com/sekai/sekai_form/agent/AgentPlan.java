package com.sekai.sekai_form.agent;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AgentPlan {
    private final String goal;
    private final Set<String> expectedTools = new LinkedHashSet<>();
    private int iteration;
    private String replanReason = "initial";

    public AgentPlan(String goal) { this.goal = goal == null ? "" : goal; }
    public String getGoal() { return goal; }
    public Set<String> getExpectedTools() { return expectedTools; }
    public int getIteration() { return iteration; }
    public String getReplanReason() { return replanReason; }
    public void markIteration(int value) { iteration = value; }
    public void replan(String reason) { replanReason = reason == null ? "tool result received" : reason; }
}
