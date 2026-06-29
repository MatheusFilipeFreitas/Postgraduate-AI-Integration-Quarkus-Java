package com.mathffreitas.travel.ai;

import com.mathffreitas.travel.ai.guard.InjectionGuard;
import com.mathffreitas.travel.ai.instructions.AssistentInstructions;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.InputGuardrails;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

// This annotation tells Quarkus to create a class that implements this interface
@RegisterAiService
public interface TravelAgentAssistent {

    @SystemMessage(AssistentInstructions.SystemMessageTravel)
    @McpToolBox("booking-server")
    @UserMessage(AssistentInstructions.UserMessageTravel)
    @InputGuardrails(InjectionGuard.class)
    String chat(@MemoryId String memoryId, @V("userMessage") String userMessage, @V("username") String username);
}
