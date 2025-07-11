package com.kousenit;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Conversation {

    private static final String DEFAULT_PREMISE = """
            They say that all operas are about a soprano
            who wants to sleep with the tenor, but the
            baritone won't let her. See, for example, La Traviata,
            Rigoletto, or Carmen.
            
            You are composing the libretto for such an opera.
            
            The setting is the wild jungles of Connecticut,
            in the not-so-distant future after global warming has
            reclaimed the land. The soprano is an intrepid
            explorer searching for the lost city of Hartford.
            The tenor is a native poet who has been living in
            the jungle for years, writing sonnets to the trees and
            composing symphonies for the monkeys.
            
            The baritone is a government agent who has been sent
            to stop the soprano from finding the lost city. He
            has a secret weapon: a giant robot that can sing
            Verdi arias in three different languages.
            
            The soprano and the tenor meet in the jungle and
            fall in love. They decide to join forces and find
            the lost city together. But the baritone is always
            one step behind them, and his giant robot is getting
            closer and closer.
            """;

    private static final Pattern SCENE_PATTERN = Pattern.compile("Scene\\s+(\\d+):\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);

    public final ChatModel gpt41 = AiModels.GPT_4_1;

    public final ChatModel claude = AiModels.CLAUDE_SONNET_4;

    public Opera generateOpera(String title, int numberOfScenes) {
        return generateOpera(title, DEFAULT_PREMISE, numberOfScenes);
    }

    public Opera generateOpera(String title, String premise, int numberOfScenes) {
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        List<Opera.Scene> scenes = new ArrayList<>();

        // First, ask for the opera title if not provided
        if (title == null || title.isBlank()) {
            memory.add(SystemMessage.from(premise));
            memory.add(UserMessage.from("""
                    Please suggest a creative and evocative title for this opera.
                    The title should capture the essence of the story - perhaps something about
                    the lost city of Hartford, the Connecticut jungle, or the romantic conflict.
                    Please provide just the title, nothing else.
                    """));
            ChatResponse titleResponse = gpt41.chat(memory.messages());
            title = extractTitle(titleResponse.aiMessage().text());
            memory.clear();
        }

        // Set up the premise
        memory.add(SystemMessage.from(premise + """
                
                When writing scenes, please format them as:
                Scene X: [Scene Title]
                [Scene content]
                """));

        UserMessage userMessage = UserMessage.from("Please write the next scene.");
        memory.add(userMessage);

        ChatModel model;
        for (int i = 0; i < numberOfScenes; i++) {
            model = i % 2 == 0 ? gpt41 : claude;
            String modelName = model == gpt41 ? "GPT-4.1" : "Claude Sonnet 4";

            ChatResponse response = model.chat(memory.messages());
            String responseText = response.aiMessage().text();

            System.out.printf("--------- %s ---------%n", modelName);
            System.out.println(responseText);

            // Parse the scene from the response
            Opera.Scene scene = parseScene(i + 1, responseText, modelName);
            scenes.add(scene);

            memory.add(response.aiMessage());
            memory.add(userMessage);
        }

        return new Opera(title, premise, scenes);
    }

    private String extractTitle(String response) {
        // Try to extract a quoted title or the first line
        Pattern titlePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = titlePattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Fallback: use first line or a default
        String[] lines = response.split("\n");
        return lines.length > 0 && !lines[0].isBlank()
                ? lines[0].replaceAll("[^\\w\\s]", "").trim()
                : "The Jungle Opera";
    }

    private Opera.Scene parseScene(int sceneNumber, String content, String author) {
        Matcher matcher = SCENE_PATTERN.matcher(content);

        if (matcher.find()) {
            // Use the scene number from the response if available
            int number = sceneNumber;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Use our counter if parsing fails
            }

            String title = matcher.group(2).trim();
            String sceneContent = content.substring(matcher.end()).trim();

            return new Opera.Scene(number, title, author, sceneContent);
        } else {
            // Fallback: treat the whole content as the scene
            String title = "Untitled Scene " + sceneNumber;
            return new Opera.Scene(sceneNumber, title, author, content);
        }
    }
}
