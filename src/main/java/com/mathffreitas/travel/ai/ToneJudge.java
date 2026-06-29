package com.mathffreitas.travel.ai;

import com.mathffreitas.travel.ai.instructions.AssistentInstructions;
import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface ToneJudge {

    @SystemMessage(AssistentInstructions.SystemMessageTone)
    boolean isProfessional(String text);
}
