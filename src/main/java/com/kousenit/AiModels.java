package com.kousenit;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

@SuppressWarnings("unused")
public class AiModels {
    // OpenAI Models (upgraded to GPT-5.2)
    public static final ChatModel GPT_5_2 = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName("gpt-5.2")
            .timeout(Duration.ofMinutes(2))
            .maxRetries(4)
            .build();

    public static final ChatModel GPT_5_MINI = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName("gpt-5-mini")
            .timeout(Duration.ofMinutes(2))
            .maxRetries(4)
            .build();

    // Anthropic Models (upgraded to Opus 4.6)
    public static final ChatModel CLAUDE_OPUS_4_6 = AnthropicChatModel.builder()
            .apiKey(ApiKeys.ANTHROPIC_API_KEY)
            .modelName("claude-opus-4-6")
            .logRequests(true)
            .logResponses(true)
            .build();

    // High-token variant for supervisor/planner use cases where large content must be passed
    // Logging disabled to reduce output verbosity
    public static final ChatModel CLAUDE_OPUS_4_6_LARGE = AnthropicChatModel.builder()
            .apiKey(ApiKeys.ANTHROPIC_API_KEY)
            .modelName("claude-opus-4-6")
            .maxTokens(8192)
            .build();

    // Google Gemini Models (upgraded to Gemini 3.1)
    public static final ChatModel GEMINI_3_1_FLASH = GoogleAiGeminiChatModel.builder()
            .apiKey(ApiKeys.GOOGLEAI_API_KEY)
            .modelName("gemini-3.1-flash-preview")
            .build();

    public static final ChatModel GEMINI_3_1_PRO = GoogleAiGeminiChatModel.builder()
            .apiKey(ApiKeys.GOOGLEAI_API_KEY)
            .modelName("gemini-3.1-pro-preview")
            .build();

    // Legacy aliases for backwards compatibility (will be removed in future version)
    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel GPT_5 = GPT_5_2;

    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel CLAUDE_OPUS_4_5 = CLAUDE_OPUS_4_6;

    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel CLAUDE_OPUS_4_1 = CLAUDE_OPUS_4_6;

    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel GEMINI_3_FLASH = GEMINI_3_1_FLASH;

    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel GEMINI_3_PRO = GEMINI_3_1_PRO;

    @Deprecated(since = "1.0", forRemoval = true)
    public static final ChatModel GEMINI_FLASH_25 = GEMINI_3_1_FLASH;
}
