package com.mathffreitas.travel.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ChatMemoryConfig {

    // creates a bean of ChatMemory for each new chat session
    @Produces
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20) // keep only the 20 last messages in memory
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .build();
    }
}
