package com.mathffreitas.travel.ai.guard;

import com.mathffreitas.travel.ai.PromptSecurityAssistent;
import com.mathffreitas.travel.ai.instructions.AssistentInstructions;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InjectionGuard implements InputGuardrail {

    @Inject
    PromptSecurityAssistent promptSecurityAssistent;

    @Override
    public InputGuardrailResult validate(UserMessage message) {
        if (promptSecurityAssistent.isAttack(message.singleText())) {
            return failure(AssistentInstructions.GuardrailFailureMessage);
        }
        return InputGuardrailResult.success();
    }
}
