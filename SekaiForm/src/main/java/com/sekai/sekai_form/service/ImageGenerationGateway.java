package com.sekai.sekai_form.service;

import com.sekai.sekai_form.agent.AgentAttachment;
import com.sekai.sekai_form.dataobject.Live2DChatConfigDO;

public interface ImageGenerationGateway {
    String generate(Live2DChatConfigDO config, String prompt);
    String edit(Live2DChatConfigDO config, String prompt, AgentAttachment source);
}
