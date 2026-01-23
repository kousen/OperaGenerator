package com.kousenit.agentic;

import com.kousenit.AiModels;
import com.kousenit.Conversation;
import com.kousenit.Opera;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agentic Opera Generator - demonstrates langchain4j-agentic patterns.
 *
 * This implementation uses a Supervisor Agent pattern where:
 * - The supervisor orchestrates the overall workflow
 * - Scene writers (GPT-5.2 and Claude Opus 4.5) generate diverse content
 * - Tools handle utility operations (saving, image generation, etc.)
 *
 * For educational demonstrations at conferences and training courses.
 */
public class AgenticOperaGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AgenticOperaGenerator.class);
    private static final Pattern SCENE_PATTERN = Pattern.compile(
            "Scene\\s+(\\d+):\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);

    private final OperaTools operaTools;
    private final SceneWriterAgent gptWriter;
    private final SceneWriterAgent claudeWriter;
    private final ChatModel supervisorModel;

    public AgenticOperaGenerator() {
        this.operaTools = new OperaTools();

        // Create scene writers with different models for diversity
        this.gptWriter = AiServices.builder(SceneWriterAgent.class)
                .chatModel(AiModels.GPT_5_2)
                .build();

        this.claudeWriter = AiServices.builder(SceneWriterAgent.class)
                .chatModel(AiModels.CLAUDE_OPUS_4_5)
                .build();

        // Use a capable model for supervision/planning
        this.supervisorModel = AiModels.CLAUDE_OPUS_4_5;
    }

    /**
     * Generate a complete opera using the agentic approach.
     *
     * @param title The opera title (or null to auto-generate)
     * @param numberOfScenes Number of scenes to generate
     * @return The generated Opera
     */
    public Opera generateOpera(String title, int numberOfScenes) {
        return generateOpera(title, Conversation.defaultPremise(), numberOfScenes);
    }

    /**
     * Generate a complete opera with a custom premise.
     *
     * @param title The opera title
     * @param premise The opera premise/setting
     * @param numberOfScenes Number of scenes to generate
     * @return The generated Opera
     */
    public Opera generateOpera(String title, String premise, int numberOfScenes) {
        logger.info("🎭 Starting agentic opera generation...");
        logger.info("   Title: {}", title != null ? title : "(auto-generate)");
        logger.info("   Scenes: {}", numberOfScenes);

        // Generate title if not provided
        if (title == null || title.isBlank()) {
            title = generateTitle(premise);
            logger.info("   Generated title: {}", title);
        }

        // Generate scenes with alternating models for diversity
        List<Opera.Scene> scenes = generateScenes(title, premise, numberOfScenes);

        // Create the opera
        Opera opera = new Opera(title, premise, scenes);
        operaTools.setCurrentOpera(opera);

        logger.info("✅ Opera generation complete: {} scenes", scenes.size());
        return opera;
    }

    /**
     * Generate scenes using alternating AI models for creative diversity.
     */
    private List<Opera.Scene> generateScenes(String title, String premise, int numberOfScenes) {
        List<Opera.Scene> scenes = new ArrayList<>();
        StringBuilder previousScenes = new StringBuilder();

        for (int i = 0; i < numberOfScenes; i++) {
            int sceneNumber = i + 1;
            boolean isFinalScene = sceneNumber == numberOfScenes;

            // Alternate between GPT and Claude for diversity
            SceneWriterAgent writer = (i % 2 == 0) ? gptWriter : claudeWriter;
            String modelName = (i % 2 == 0) ? "GPT-5.2" : "Claude Opus 4.5";

            String sceneInstructions = isFinalScene
                    ? "This is the FINAL scene. Tie off major plot threads, resolve the romantic arc, and deliver a decisive ending."
                    : "Set the stage for upcoming scenes so the opera can naturally progress to its finale.";

            logger.info("📝 Generating Scene {} with {}...", sceneNumber, modelName);

            String sceneContent = writer.writeScene(
                    title,
                    premise,
                    previousScenes.toString(),
                    sceneNumber,
                    numberOfScenes,
                    sceneInstructions
            );

            // Parse the scene
            Opera.Scene scene = parseScene(sceneNumber, sceneContent, modelName);
            scenes.add(scene);

            // Add to context for next scene
            previousScenes.append("\n--- Previous Scene ---\n");
            previousScenes.append(sceneContent);
            previousScenes.append("\n");

            logger.info("   ✓ Scene {}: \"{}\" by {}", scene.number(), scene.title(), scene.author());
        }

        return scenes;
    }

    /**
     * Generate an opera title using GPT.
     */
    private String generateTitle(String premise) {
        // Use the scene writer to generate a title (reusing the interface)
        String response = gptWriter.writeScene(
                "TITLE_GENERATION",
                premise,
                "",
                0,
                0,
                """
                        Instead of writing a scene, please suggest a creative and evocative
                        title for this opera. The title should capture the essence of the story.
                        Respond with ONLY the title, nothing else.
                        """
        );

        // Clean up the response
        String title = response.trim()
                .replaceAll("^[\"']|[\"']$", "")  // Remove quotes
                .replaceAll("Scene.*?:", "")       // Remove any scene prefix
                .trim();

        return title.isEmpty() ? "The Opera of Hartford" : title;
    }

    /**
     * Parse scene content into a Scene object.
     */
    private Opera.Scene parseScene(int expectedNumber, String content, String author) {
        Matcher matcher = SCENE_PATTERN.matcher(content);

        if (matcher.find()) {
            int number = expectedNumber;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Use expected number
            }
            String title = matcher.group(2).trim();
            String sceneContent = content.substring(matcher.end()).trim();
            return new Opera.Scene(number, title, author, sceneContent);
        }

        // Fallback
        return new Opera.Scene(expectedNumber, "Untitled Scene " + expectedNumber, author, content);
    }

    /**
     * Run the complete production workflow after generating the opera.
     */
    public void runProductionWorkflow() {
        if (operaTools.getCurrentOpera() == null) {
            logger.error("No opera generated. Call generateOpera() first.");
            return;
        }

        logger.info("🎬 Running production workflow...");

        // Step 1: Save libretto
        logger.info("💾 Saving libretto...");
        String saveResult = operaTools.saveLibretto();
        logger.info("   {}", saveResult);

        // Step 2: Generate images (parallel)
        logger.info("🎨 Generating illustrations...");
        String imageResult = operaTools.generateAllImages();
        logger.info("   {}", imageResult);

        // Step 3: Generate narration (optional)
        logger.info("🎙️ Generating narration...");
        String narrationResult = operaTools.generateNarration();
        logger.info("   {}", narrationResult);

        // Step 4: Generate critique (optional)
        logger.info("📰 Generating critique...");
        String critiqueResult = operaTools.generateCritique();
        logger.info("   {}", critiqueResult);

        // Step 5: Prepare exports
        logger.info("📦 Preparing exports...");
        String exportResult = operaTools.prepareExports();
        logger.info("   {}", exportResult);

        // Final status
        logger.info("✅ Production workflow complete!");
        logger.info(operaTools.getStatus());
    }

    /**
     * Get the tools instance for direct access.
     */
    public OperaTools getTools() {
        return operaTools;
    }

    /**
     * Main entry point for demonstration.
     */
    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("  🎭 AGENTIC OPERA GENERATOR");
        System.out.println("  Demonstrating langchain4j-agentic patterns");
        System.out.println("═".repeat(60));
        System.out.println();

        String title = args.length > 0 ? args[0] : null;
        int numberOfScenes = args.length > 1 ? Integer.parseInt(args[1]) : 5;

        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        // Generate the opera
        Opera opera = generator.generateOpera(title, numberOfScenes);

        System.out.println();
        System.out.println("─".repeat(60));
        System.out.printf("Generated: \"%s\" with %d scenes%n", opera.title(), opera.scenes().size());
        System.out.println("─".repeat(60));
        System.out.println();

        // Run production workflow
        generator.runProductionWorkflow();

        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("  🎉 COMPLETE");
        System.out.println("═".repeat(60));
    }
}
