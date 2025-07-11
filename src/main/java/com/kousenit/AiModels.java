package com.kousenit;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

@SuppressWarnings("unused")
public class AiModels {
    public static final ChatModel GPT_4_1 = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName(OpenAiChatModelName.GPT_4_1)
            .build();

    public static final ChatModel CLAUDE_SONNET_4 = AnthropicChatModel.builder()
            .apiKey(ApiKeys.ANTHROPIC_API_KEY)
            .modelName("claude-sonnet-4-20250514")
            .logRequests(true)
            .logResponses(true)
            .build();

    public static final ChatModel GEMINI_FLASH_25 = GoogleAiGeminiChatModel.builder()
            .apiKey(ApiKeys.GOOGLEAI_API_KEY)
            .modelName("gemini-2.5-flash")
            .build();
}