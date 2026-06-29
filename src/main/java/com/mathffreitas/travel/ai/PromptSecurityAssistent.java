package com.mathffreitas.travel.ai;

import com.mathffreitas.travel.ai.instructions.AssistentInstructions;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface PromptSecurityAssistent {

    @SystemMessage(value = AssistentInstructions.SystemMessageSecurity)
    @UserMessage(AssistentInstructions.UserMessageSecurity)
    boolean isAttack(@V("message") String message);

}
