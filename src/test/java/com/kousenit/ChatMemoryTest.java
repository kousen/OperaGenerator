package com.kousenit;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChatMemoryTest {

    @Test
    void statelessRequests() {
        String response1 = AiModels.GPT_4_1.chat("""
                Hello. My name is Inigo Montoya.
                You killed my father.
                Prepare to die.
                """);
        String response2 = AiModels.GPT_4_1.chat(
                "What's my name?");
        System.out.println("Response 1: " + response1);
        System.out.println("\nResponse 2: " + response2);

        assertFalse(response2.contains("Inigo"));
    }

    interface Assistant {
        String ask(String question);
    }

    @Test
    void statefulRequestsForSingleLLM() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .chatModel(AiModels.GPT_4_1)
                .build();

        String response1 = assistant.ask("""
                Hello. My name is Inigo Montoya.
                You killed my father.
                Prepare to die.
                """);
        String response2 = assistant.ask(
                "What's my name?");
        System.out.println("Response 1: " + response1);
        System.out.println("\nResponse 2: " + response2);

        assertTrue(response2.contains("Inigo"));
    }
}
