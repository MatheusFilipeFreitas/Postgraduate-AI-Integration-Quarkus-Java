package com.mathffreitas;

import io.quarkiverse.langchain4j.RegisterAiService;

// This annotation tells Quarkus to create a class that implements this interface
@RegisterAiService
public interface TravelAgentAssistent {

    /**
     * Method 'chat' receives the user message and returns LLM response
     * @param userMessage user message
     * @return LLM response
     */
    String chat(String userMessage);
}
