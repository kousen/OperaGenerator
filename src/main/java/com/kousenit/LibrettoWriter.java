package com.kousenit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LibrettoWriter {

    static String RESOURCE_PATH = "src/main/resources"; // Package-private for testing

    // Pattern to match character singing lines (CHARACTER (description): or CHARACTER:)
    private static final Pattern SINGING_PATTERN = Pattern.compile(
            "^([A-Z][A-Z\\s&]+)\\s*\\(([^)]+)\\):\\s*$|^([A-Z][A-Z\\s&]+):\\s*$",
            Pattern.MULTILINE
    );

    /**
     * Automatically formats scene content to use Option 1 stanza formatting.
     * Converts character singing lines to use blockquotes with proper line breaks.
     */
    private static String formatSceneContent(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inLyricSection = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Check if this line starts a new singing section
            Matcher matcher = SINGING_PATTERN.matcher(line);
            if (matcher.matches()) {
                String character = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
                String description = matcher.group(2);

                // Determine voice type based on character name
                String voiceType = determineVoiceType(character);

                // Format the character line
                if (description != null && !description.trim().isEmpty()) {
                    result.append("**").append(character.trim()).append("** (")
                            .append(voiceType).append(", ").append(description.trim()).append("):\n");
                } else {
                    result.append("**").append(character.trim()).append("** (")
                            .append(voiceType).append("):\n");
                }

                inLyricSection = true;
                continue;
            }

            // Check if we're leaving a lyric section 
            if (inLyricSection && (trimmed.startsWith("[") || trimmed.isEmpty() ||
                                   trimmed.startsWith("#") || trimmed.startsWith(">"))) {
                inLyricSection = false;
                result.append(line).append("\n");
                continue;
            }

            // If we're in a lyric section and this looks like a lyric line
            if (inLyricSection && !trimmed.startsWith("[") && !trimmed.startsWith("#") &&
                !trimmed.startsWith("**") && !trimmed.startsWith(">")) {

                // Format as blockquote with line break (except for last line detection)
                result.append("> ").append(trimmed);

                // Add <br> unless this appears to be the last line of a stanza
                // (we'll add <br> to all lines for consistency)
                result.append("<br>\n");
                continue;
            }

            // Default: keep line as-is
            result.append(line).append("\n");
        }

        // Clean up any trailing <br> tags that might be at the end of stanzas
        return result.toString().replaceAll("<br>\n(\\s*\n)", "\n$1");
    }

    /**
     * Determines the voice type based on character name.
     * This uses common opera character naming conventions.
     */
    private static String determineVoiceType(String character) {
        String name = character.toLowerCase();

        // Common character patterns
        if (name.contains("sandra") || name.contains("soprano")) return "soprano";
        if (name.contains("lucian") || name.contains("tenor")) return "tenor";
        if (name.contains("maximilian") || name.contains("baritone")) return "baritone";
        if (name.contains("robot") || name.contains("bass")) return "bass";
        if (name.contains("helena") || name.contains("mezzo")) return "mezzo-soprano";
        if (name.contains("all") || name.contains("chorus")) return "ensemble";

        // Default assignments for common opera roles
        if (name.contains("agent") || name.contains("government")) return "baritone";
        if (name.contains("explorer") || name.contains("woman")) return "soprano";
        if (name.contains("poet") || name.contains("man")) return "tenor";
        if (name.contains("council") || name.contains("citizens")) return "chorus";

        return "voice"; // Generic fallback
    }

    public static Path saveLibretto(Opera opera) throws IOException {
        String filename = opera.title().replaceAll("\\s+", "_").toLowerCase() + "_libretto.md";
        Path outputPath = Paths.get(RESOURCE_PATH, filename);

        StringBuilder markdown = new StringBuilder();

        // Add title and premise
        markdown.append("# ").append(opera.title()).append("\n\n");
        markdown.append("## Premise\n\n");
        markdown.append(opera.premise()).append("\n\n");
        markdown.append("---\n\n");

        // Add each scene
        for (Opera.Scene scene : opera.scenes()) {
            markdown.append("### Scene ").append(scene.number())
                    .append(": ").append(scene.title()).append("\n\n");

            // Add author attribution
            markdown.append("> **Author: ").append(scene.author()).append("**\n\n");

            // Add scene content with automatic formatting
            markdown.append(formatSceneContent(scene.content())).append("\n\n");

            // Add separator between scenes
            markdown.append("---\n\n");
        }

        // Ensure directory exists
        Files.createDirectories(outputPath.getParent());

        // Write the file
        Files.writeString(outputPath, markdown.toString());
        System.out.println("Libretto saved to: " + outputPath);

        return outputPath;
    }

    public static void saveScenesToFiles(Opera opera) throws IOException {
        Path scenesDir = Paths.get(RESOURCE_PATH);
        Files.createDirectories(scenesDir);

        for (Opera.Scene scene : opera.scenes()) {
            Path scenePath = scenesDir.resolve(scene.getFileName());

            String content = String.format("""
                    Scene %d: %s
                    Author: %s
                    
                    %s
                    """, scene.number(), scene.title(), scene.author(), formatSceneContent(scene.content()));

            Files.writeString(scenePath, content);
            System.out.println("Scene saved to: " + scenePath);
        }
    }

    /**
     * Saves opera with organized directory structure and complete libretto.
     * Creates directory named after the opera and saves all files there.
     */
    public static Path saveCompleteOpera(Opera opera) throws IOException {
        // Create organized directory name
        String folderName = opera.title().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
        Path operaDir = Paths.get(RESOURCE_PATH, folderName);
        Files.createDirectories(operaDir);

        // Save complete libretto with all scenes formatted
        String librettoFilename = folderName + "_complete_libretto.md";
        Path librettoPath = operaDir.resolve(librettoFilename);

        StringBuilder markdown = new StringBuilder();

        // Add title and premise
        markdown.append("# ").append(opera.title()).append("\n\n");
        markdown.append("## Premise\n\n");
        markdown.append(opera.premise()).append("\n\n");
        markdown.append("---\n\n");

        // Add each scene with image placeholders
        for (Opera.Scene scene : opera.scenes()) {
            markdown.append("### Scene ").append(scene.number())
                    .append(": ").append(scene.title()).append("\n\n");

            // Add image placeholder
            markdown.append("![Scene ").append(scene.number())
                    .append(" Illustration](scene_").append(scene.number())
                    .append("_illustration.png)\n\n\n\n");

            // Add author attribution
            markdown.append("> **Author: ").append(scene.author()).append("**\n\n");

            // Add formatted scene content
            markdown.append(formatSceneContent(scene.content())).append("\n\n");

            // Add separator between scenes
            markdown.append("---\n\n");
        }

        Files.writeString(librettoPath, markdown.toString());
        System.out.println("Complete libretto saved to: " + librettoPath);

        // Save individual scene files to the opera directory  
        for (Opera.Scene scene : opera.scenes()) {
            Path scenePath = operaDir.resolve(scene.getFileName());

            String content = String.format("""
                    Scene %d: %s
                    Author: %s
                    
                    %s
                    """, scene.number(), scene.title(), scene.author(), formatSceneContent(scene.content()));

            Files.writeString(scenePath, content);
            System.out.println("Scene saved to: " + scenePath);
        }

        return librettoPath;
    }
}