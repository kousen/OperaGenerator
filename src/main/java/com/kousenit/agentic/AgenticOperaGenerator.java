package com.kousenit.agentic;

import com.kousenit.AiModels;
import com.kousenit.Conversation;
import com.kousenit.Opera;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agentic Opera Generator - demonstrates langchain4j-agentic patterns.
 * <p>
 * This class showcases TWO approaches for educational comparison:
 * <p>
 * 1. MANUAL ORCHESTRATION (generateOpera + runProductionWorkflow):
 *    - Your code explicitly controls the sequence of operations
 *    - Predictable, debuggable, but inflexible
 * <p>
 * 2. TRUE AGENTIC (generateOperaAutonomously):
 *    - The supervisor agent DECIDES what to do and when
 *    - The LLM plans, executes, and adapts autonomously
 *    - Demonstrates the power of agentic AI systems
 */
@SuppressWarnings("FieldCanBeLocal")
public class AgenticOperaGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AgenticOperaGenerator.class);
    private static final Pattern SCENE_PATTERN = Pattern.compile(
            "Scene\\s+(\\d+):\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);

    private final OperaTools operaTools;
    private final SceneWriterAgent gptWriter;
    private final SceneWriterAgent claudeWriter;
    private final ProductionAgent productionAgent;
    private final SupervisorAgent supervisor;
    private final ChatModel supervisorModel;

    public AgenticOperaGenerator() {
        this.operaTools = new OperaTools();
        // Use high-token model for supervisor - planner needs to pass large scene content in JSON
        this.supervisorModel = AiModels.CLAUDE_OPUS_4_5_LARGE;

        // Create scene writers with different models for diversity
        // Using AgenticServices.agentBuilder() for supervisor compatibility
        this.gptWriter = AgenticServices
                .agentBuilder(SceneWriterAgent.class)
                .chatModel(AiModels.GPT_5_2)
                .name("gptSceneWriter")
                .description("""
                    Writes opera scenes using GPT-5.2 with a creative, dramatic style""")
                .build();

        this.claudeWriter = AgenticServices
                .agentBuilder(SceneWriterAgent.class)
                .chatModel(AiModels.CLAUDE_OPUS_4_5)
                .name("claudeSceneWriter")
                .description("""
                    Writes opera scenes using Claude Opus 4.5 with a lyrical, nuanced style""")
                .build();

        // Create production agent with access to tools
        this.productionAgent = AgenticServices
                .agentBuilder(ProductionAgent.class)
                .chatModel(supervisorModel)
                .name("productionAgent")
                .description("""
                    Handles production tasks: saving libretto, generating images,
                    narration, critique""")
                .tools(operaTools)
                .build();

        // Build the TRUE agentic supervisor
        // This supervisor AUTONOMOUSLY decides which agents to call and when
        this.supervisor = AgenticServices.supervisorBuilder()
                .chatModel(supervisorModel)
                .name("OperaSupervisor")
                .description("Orchestrates the creation of complete AI-generated operas")
                .subAgents(gptWriter, claudeWriter, productionAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .maxAgentsInvocations(20)  // Allow enough calls for multi-scene opera
                .supervisorContext("""
                        You are orchestrating opera creation. Your sub-agents are:

                        1. gptSceneWriter - Writes scenes using GPT-5.2
                           Use for odd-numbered scenes (1, 3, 5...)

                        2. claudeSceneWriter - Writes scenes using Claude Opus 4.5
                           Use for even-numbered scenes (2, 4, 6...)

                        3. productionAgent - Handles production tasks including:
                           - registerScene: Register each scene after it's written
                           - buildOpera: Build the Opera object from registered scenes
                           - saveLibretto, generateAllImages, generateNarration,
                           generateCritique, prepareExports

                        WORKFLOW (follow this exact sequence):
                        1. Write Scene 1 with gptSceneWriter
                        2. Call productionAgent with: "Register scene 1 with the
                            following COMPLETE content: [paste ENTIRE scene here].
                            Author: GPT-5.2"
                        3. Write Scene 2 with claudeSceneWriter
                        4. Call productionAgent with: "Register scene 2 with the
                            following COMPLETE content: [paste ENTIRE scene here].
                            Author: Claude Opus 4.5"
                        5. Continue alternating until all scenes are written and registered
                        6. Call productionAgent to buildOpera(title, premise)
                        7. Call productionAgent to run the full production workflow
                            (saveLibretto, generateAllImages, etc.)

                        CRITICAL REQUIREMENTS:
                        - You MUST pass the COMPLETE scene text to registerScene, not a summary
                        - Include ALL dialogue, stage directions, and character lines
                        - The full scene content is required for the libretto
                        - You MUST call buildOpera BEFORE running production tasks
                        - Maintain narrative continuity by passing previous scenes to writers
                        - Mark final scenes with closure instructions
                        """)
                .build();
    }

    // ========================================================================
    // TRUE AGENTIC APPROACH - The supervisor decides everything autonomously
    // ========================================================================

    /**
     * Generate an opera using TRUE agentic orchestration with the default premise.
     * <p>
     * Uses the classic opera premise from {@link Conversation#defaultPremise()}.
     *
     * @return The supervisor's summary of what was accomplished
     */
    public String generateOperaAutonomously() {
        String defaultRequest = String.format("""
                Create a 2-scene opera with the following premise:

                %s

                Use alternating writing styles between scenes.
                After the scenes are complete, run the full production workflow.""",
                Conversation.defaultPremise());
        return generateOperaAutonomously(defaultRequest);
    }

    /**
     * Generate an opera using TRUE agentic orchestration.
     * <p>
     * The supervisor agent autonomously:
     * - Plans the workflow
     * - Decides which scene writer to use for each scene
     * - Maintains continuity between scenes
     * - Runs production when scenes are complete
     * - Reports progress and completion
     *
     * @param request Natural language request like "Create a 3-scene opera about AI and humanity"
     * @return The supervisor's summary of what was accomplished
     */
    public String generateOperaAutonomously(String request) {
        logger.info("═".repeat(60));
        logger.info("  🤖 TRUE AGENTIC MODE - Supervisor takes control");
        logger.info("═".repeat(60));
        logger.info("Request: {}", request);
        logger.info("");

        // The supervisor autonomously decides everything from here
        String result = supervisor.invoke(request);

        logger.info("");
        logger.info("═".repeat(60));
        logger.info("  ✅ AUTONOMOUS GENERATION COMPLETE");
        logger.info("═".repeat(60));

        return result;
    }

    // ========================================================================
    // MANUAL ORCHESTRATION - Your code controls the sequence
    // ========================================================================

    /**
     * Generate a complete opera using MANUAL orchestration.
     * Your code explicitly controls the sequence of operations.
     *
     * @param title The opera title (or null to auto-generate)
     * @param numberOfScenes Number of scenes to generate
     * @return The generated Opera
     */
    public Opera generateOpera(String title, int numberOfScenes) {
        return generateOpera(title, Conversation.defaultPremise(), numberOfScenes);
    }

    /**
     * Generate a complete opera with a custom premise using MANUAL orchestration.
     *
     * @param title The opera title
     * @param premise The opera premise/setting
     * @param numberOfScenes Number of scenes to generate
     * @return The generated Opera
     */
    public Opera generateOpera(String title, String premise, int numberOfScenes) {
        logger.info("─".repeat(60));
        logger.info("  📋 MANUAL MODE - Code controls the workflow");
        logger.info("─".repeat(60));
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

        logger.info("✅ Manual opera generation complete: {} scenes", scenes.size());
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

        String title = response.trim()
                .replaceAll("^[\"']|[\"']$", "")
                .replaceAll("Scene.*?:", "")
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

        return new Opera.Scene(expectedNumber, "Untitled Scene " + expectedNumber, author, content);
    }

    /**
     * Run the complete production workflow after generating the opera.
     * This is the MANUAL approach - your code decides the sequence.
     */
    public void runProductionWorkflow() {
        if (operaTools.getCurrentOpera() == null) {
            logger.error("No opera generated. Call generateOpera() first.");
            return;
        }

        logger.info("🎬 Running MANUAL production workflow...");

        logger.info("💾 Step 1: Saving libretto...");
        logger.info("   {}", operaTools.saveLibretto());

        logger.info("🎨 Step 2: Generating illustrations...");
        logger.info("   {}", operaTools.generateAllImages());

        logger.info("🎙️ Step 3: Generating narration...");
        logger.info("   {}", operaTools.generateNarration());

        logger.info("📰 Step 4: Generating critique...");
        logger.info("   {}", operaTools.generateCritique());

        logger.info("📦 Step 5: Preparing exports...");
        logger.info("   {}", operaTools.prepareExports());

        logger.info("✅ Manual production workflow complete!");
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
     * <p>
     * Usage:
     *   java AgenticOperaGenerator                                    # Agentic mode (default)
     *   java AgenticOperaGenerator "Create a 3-scene opera about X"   # Agentic mode, custom request
     *   java AgenticOperaGenerator --manual                           # Manual mode, 5 scenes
     *   java AgenticOperaGenerator --manual "Title" 3                 # Manual mode, custom title, 3 scenes
     */
    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("  🎭 AGENTIC OPERA GENERATOR");
        System.out.println("  Demonstrating langchain4j-agentic patterns");
        System.out.println("═".repeat(60));
        System.out.println();

        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        // Check for manual mode
        if (args.length > 0 && args[0].equals("--manual")) {
            // MANUAL MODE - code controls the workflow
            String title = args.length > 1 ? args[1] : null;
            int numberOfScenes = args.length > 2 ? Integer.parseInt(args[2]) : 5;

            System.out.println("📋 MANUAL MODE: Code controls the workflow");
            System.out.println();

            Opera opera = generator.generateOpera(title, numberOfScenes);

            System.out.println();
            System.out.println("─".repeat(60));
            System.out.printf("Generated: \"%s\" with %d scenes%n", opera.title(), opera.scenes().size());
            System.out.println("─".repeat(60));
            System.out.println();

            generator.runProductionWorkflow();
        } else {
            // AGENTIC MODE (default) - supervisor decides everything
            String request = args.length > 0
                    ? args[0]
                    : "Create a 2-scene opera about the struggle between AI and human creativity. " +
                      "Use alternating writing styles. After the scenes are complete, run the full production workflow.";

            System.out.println("🤖 AGENTIC MODE: Supervisor decides everything");
            System.out.println();

            String result = generator.generateOperaAutonomously(request);

            System.out.println();
            System.out.println("─".repeat(60));
            System.out.println("SUPERVISOR SUMMARY:");
            System.out.println("─".repeat(60));
            System.out.println(result);
        }

        System.out.println();
        System.out.println("═".repeat(60));
        System.out.println("  🎉 COMPLETE");
        System.out.println("═".repeat(60));
    }
}
