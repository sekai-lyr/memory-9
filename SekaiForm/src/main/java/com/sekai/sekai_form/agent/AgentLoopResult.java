package com.sekai.sekai_form.agent;

import java.util.List;

public record AgentLoopResult(boolean success, String reply, String imageUrl, String audioUrl,
                              List<ToolCallResult> toolCalls) { }
