package com.mathffreitas.travel.ai.guard;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonReader;

import java.io.StringReader;

// is not being used on app, but is a preview for a guardrail using data (can check for props of object)

@ApplicationScoped
public class JsonStructureGuard implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage message) {
        String response = message.text();
        try (JsonReader reader = Json.createReader(new StringReader(response))) {
            reader.readObject();
            return OutputGuardrailResult.success();
        } catch (Exception ex) {
            return reprompt(
                    "Response is not valid JSON: " + ex.getMessage(),
                    """
                    Your previous answer could not be parsed as JSON.
                    
                    Regenerate the answer with these rules:
                    - Return one JSON object only
                    - Do not wrap the JSON in markdown code fences (no ```json blocks)
                    - Do not add explanations, comments, or any text before or after the JSON
                    - Use double quotes for all keys and string values
                    - Ensure brackets, commas, and colons are valid and the JSON is complete
                    """
            );
        }
    }
}
