package com.mathffreitas.travel.ai.guard;

import com.mathffreitas.travel.ai.ToneJudge;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ToneGuardrail implements OutputGuardrail {

    @Inject
    ToneJudge judge;

    @Override
    public OutputGuardrailResult validate(AiMessage message) {
        if (!judge.isProfessional(message.text())) {
            return reprompt(
                    "Response tone is not professional.",
                    """
                    Your previous answer was flagged as rude, dismissive, or too informal for customer service.
                    
                    Rewrite the answer with these rules:
                    - Use a polite, respectful, and empathetic tone
                    - Stay professional, as a senior travel agent at World Trips would
                    - Be clear and helpful without slang, sarcasm, or blame toward the customer
                    - Keep the same factual content and language as the customer's request
                    """
            );
        }
        return OutputGuardrailResult.success();
    }
}
