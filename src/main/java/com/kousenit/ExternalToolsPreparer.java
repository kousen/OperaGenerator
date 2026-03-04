package com.kousenit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prepares export packages for external tools that don't have APIs.
 * Creates ready-to-use content for Suno AI and NotebookLM.
 */
public class ExternalToolsPreparer {

    /**
     * Generate music prompts for Suno AI based on scene content.
     */
    public static void generateSunoPrompts(Opera opera, Path outputDir) throws IOException {
        StringBuilder prompts = new StringBuilder();
        prompts.append("# Suno AI Music Generation Prompts\n\n");
        prompts.append("## Opera: ").append(opera.title()).append("\n\n");
        prompts.append("Copy these prompts into Suno AI to generate music for each scene:\n\n");

        for (Opera.Scene scene : opera.scenes()) {
            prompts.append("### Scene ").append(scene.number())
                   .append(": ").append(scene.title()).append("\n\n");

            // Analyze scene content to suggest music style
            String musicStyle = analyzeMusicStyle(scene);
            prompts.append("**Prompt for Suno:**\n```\n");
            prompts.append(musicStyle);
            prompts.append("\n```\n\n");

            // Extract key lyrics for potential use
            String keyLyrics = extractKeyLyrics(scene);
            if (!keyLyrics.isEmpty()) {
                prompts.append("**Optional Lyrics to Include:**\n");
                prompts.append(keyLyrics).append("\n\n");
            }
        }

        // Add finale suggestion
        prompts.append("### Finale/Overture\n\n");
        prompts.append("**Prompt for Suno:**\n```\n");
        prompts.append("Grand operatic finale, full orchestra, mixed chorus, triumphant, ");
        prompts.append("incorporating themes from all ").append(opera.scenes().size())
               .append(" scenes, dramatic crescendo\n```\n");

        Path promptsFile = outputDir.resolve("suno_music_prompts.md");
        Files.writeString(promptsFile, prompts.toString());
        System.out.println("   📝 Suno AI prompts saved to: " + promptsFile.getFileName());
    }

    /**
     * Prepare a comprehensive package for NotebookLM upload.
     */
    public static void prepareNotebookLMPackage(Opera opera, Path operaDir) throws IOException {
        StringBuilder combined = new StringBuilder();

        // Create a single comprehensive document for NotebookLM
        combined.append("# Complete Opera Package for AI Analysis\n\n");
        combined.append("## Opera: ").append(opera.title()).append("\n\n");
        combined.append("### Overview\n");
        combined.append(opera.premise()).append("\n\n");

        // Add production notes
        combined.append("### Production Notes\n");
        combined.append("- Generated using GPT-5.2 and Claude Opus 4.6 in alternating collaboration\n");
        combined.append("- Total scenes: ").append(opera.scenes().size()).append("\n");
        combined.append("- Setting: Post-apocalyptic Connecticut jungle\n\n");

        // Include all scenes with analysis prompts
        combined.append("### Complete Libretto with Discussion Points\n\n");
        for (Opera.Scene scene : opera.scenes()) {
            combined.append("#### Scene ").append(scene.number())
                    .append(": ").append(scene.title()).append("\n");
            combined.append("*Author: ").append(scene.author()).append("*\n\n");
            combined.append(scene.content()).append("\n\n");

            // Add discussion prompts for the AI podcast
            combined.append("**Discussion Points for Scene ")
                    .append(scene.number()).append(":**\n");
            combined.append(generateDiscussionPoints(scene)).append("\n\n");
        }

        // Add thematic analysis section
        combined.append("### Thematic Analysis Topics\n\n");
        combined.append(generateThematicAnalysis(opera));

        // Add podcast outline
        combined.append("\n### Suggested Podcast Structure\n\n");
        combined.append(generatePodcastOutline(opera));

        Path notebookFile = operaDir.resolve("notebooklm_package.md");
        Files.writeString(notebookFile, combined.toString());
        System.out.println("   📚 NotebookLM package saved to: " + notebookFile.getFileName());

        // Also create upload instructions
        String instructions = """
            # NotebookLM Upload Instructions

            1. Go to https://notebooklm.google.com
            2. Create a new notebook
            3. Upload these files:
               - notebooklm_package.md (comprehensive opera document)
               - %s_complete_libretto.md (formatted libretto)
               - %s_critique.md (if available)
            4. Generate Audio Overview for an AI-generated podcast
            5. Use the discussion points for guided analysis

            The package includes pre-written discussion prompts and thematic
            analysis to guide the AI podcast generation.
            """.formatted(opera.title().toLowerCase().replace(" ", "_"),
                         opera.title().toLowerCase().replace(" ", "_"));

        Path instructionsFile = operaDir.resolve("notebooklm_instructions.txt");
        Files.writeString(instructionsFile, instructions);
    }

    private static String analyzeMusicStyle(Opera.Scene scene) {
        String content = scene.content().toLowerCase();
        StringBuilder style = new StringBuilder();

        // Determine vocal arrangement based on characters
        if (content.contains("sandra") && content.contains("lucian")) {
            style.append("Romantic duet, soprano and tenor, ");
        } else if (content.contains("maximilian")) {
            style.append("Dramatic baritone aria, ");
        } else if (content.contains("chorus")) {
            style.append("Full chorus with orchestra, ");
        } else {
            style.append("Operatic aria, ");
        }

        // Analyze mood
        if (content.contains("love") || content.contains("heart")) {
            style.append("romantic, lyrical, ");
        } else if (content.contains("danger") || content.contains("pursuit")) {
            style.append("tense, dramatic, fast-paced, ");
        } else if (content.contains("ancient") || content.contains("mystic")) {
            style.append("mysterious, ethereal, ");
        }

        // Setting
        style.append("jungle ambiance, post-apocalyptic opera, ");
        style.append("contemporary classical style");

        return style.toString();
    }

    private static String extractKeyLyrics(Opera.Scene scene) {
        // Extract the most dramatic sung lines (usually in quotes or after character names)
        return scene.content().lines()
            .filter(line -> line.contains(":") || line.contains(">"))
            .limit(4)
            .collect(Collectors.joining("\n"));
    }

    private static String generateDiscussionPoints(Opera.Scene scene) {
        return """
            - How does the AI collaboration between GPT-5.2 and Claude Opus 4.6 affect the narrative?
            - What operatic conventions are being followed or subverted?
            - Analysis of the character dynamics in this scene
            - The role of the setting (post-apocalyptic Connecticut) in the drama
            - Musical moments that would be most impactful
            """;
    }

    private static String generateThematicAnalysis(Opera opera) {
        return """
            1. **Technology vs. Nature**: The tension between mechanical elements and jungle overgrowth
            2. **Love in Ruins**: Romance blooming in a post-apocalyptic setting
            3. **AI Collaboration**: How two AI models create unified narrative
            4. **Environmental Commentary**: Climate change as backdrop
            5. **Classical Form, Modern Setting**: Opera conventions in sci-fi context
            6. **The Role of Memory**: Hartford as both physical and cultural ruin
            7. **Authority vs. Freedom**: Government agents vs. explorers
            """;
    }

    private static String generatePodcastOutline(Opera opera) {
        return String.format("""
            1. **Introduction** (2 min): The concept of AI-generated opera
            2. **Plot Overview** (3 min): Summary of %s
            3. **Scene Analysis** (10 min): Deep dive into key moments
            4. **Character Discussion** (5 min): Archetypes and development
            5. **AI Collaboration** (5 min): How GPT-5.2 and Claude Opus 4.6 work together
            6. **Musical Possibilities** (3 min): How this would sound on stage
            7. **Thematic Exploration** (5 min): Major themes and metaphors
            8. **Conclusion** (2 min): The future of AI in creative arts
            """, opera.title());
    }
}