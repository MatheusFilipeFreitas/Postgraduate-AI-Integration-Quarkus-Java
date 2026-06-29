package com.mathffreitas.travel.resource;

import com.mathffreitas.travel.ai.TravelAgentAssistent;
import com.mathffreitas.travel.ai.instructions.AssistentInstructions;
import dev.langchain4j.guardrail.InputGuardrailException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/travel")
public class TravelAgentResource {

    @Inject
    private TravelAgentAssistent agentAssist;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(
            String message,
            @HeaderParam("X-User-Name") @DefaultValue("Guest") String username
    ) {
        try {
            return agentAssist.chat("session-id", message, username);
        } catch (InputGuardrailException e) {
            return AssistentInstructions.GuardrailFailureMessage.trim();
        }
    }
}
