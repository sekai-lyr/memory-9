package com.sekai.sekai_form.agent.memory;

import java.util.List;

public record MemoryContext(String summary, List<MemoryMessage> recentMessages, String state) { }
