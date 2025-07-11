package com.kousenit;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ConversationTest {
    private final Conversation conversation = new Conversation();

    @Test
    void testGenerateSmallOpera() {
        // Test the new generateOpera method with a small opera
        Opera opera = conversation.generateOpera("Test Opera", 2);
        
        assertThat(opera).isNotNull();
        assertThat(opera.title()).isEqualTo("Test Opera");
        assertThat(opera.scenes()).hasSize(2);
        assertThat(opera.premise()).isNotEmpty();
        
        // Verify scenes have proper content
        for (Opera.Scene scene : opera.scenes()) {
            assertThat(scene.number()).isPositive();
            assertThat(scene.title()).isNotEmpty();
            assertThat(scene.author()).isIn("GPT-4.1", "Claude Sonnet 4");
            assertThat(scene.content()).isNotEmpty();
        }
        
        System.out.printf("✅ Generated test opera: %s with %d scenes%n", 
                         opera.title(), opera.scenes().size());
    }

    @Test
    void testGenerateOperaWithAutoTitle() {
        // Test auto-title generation (pass null for title)
        Opera opera = conversation.generateOpera(null, 1);
        
        assertThat(opera).isNotNull();
        assertThat(opera.title()).isNotEmpty().isNotEqualTo("null");
        assertThat(opera.scenes()).hasSize(1);
        
        System.out.printf("✅ Auto-generated title: %s%n", opera.title());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
    void critique() {
        Document libretto = FileSystemDocumentLoader.loadDocument(
                "src/main/resources/libretto.md");
        String query = """
                Write a detailed literary critique of the following
                libretto, from an opera entitled
                "Whispers of the Lost Hartford,"
                suitable for a newspaper review or academic journal:
                %s
                """.formatted(libretto.text());
        var gemini = AiModels.GEMINI_FLASH_25;
        ChatResponse response = gemini.chat(
                List.of(SystemMessage.from("""
                        You are an experienced music critic
                        with a deep knowledge of opera.
                        """),
                        UserMessage.from(query)));
        System.out.println(response.aiMessage().text());
        System.out.println(response.tokenUsage());
    }
}