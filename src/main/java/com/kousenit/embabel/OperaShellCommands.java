package com.kousenit.embabel;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.kousenit.Conversation;
import com.kousenit.Opera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * Spring Shell commands for the Embabel Opera Generator.
 * <p>
 * Provides two modes of operation:
 * <p>
 * 1. MANUAL MODE: Code explicitly controls the sequence
 *    - generate-opera: Generate scenes with alternating models
 *    - run-production: Execute production workflow
 * <p>
 * 2. AGENTIC MODE: GOAP planner decides the sequence
 *    - create-opera: Fully autonomous opera creation
 * <p>
 * Run with: ./gradlew bootRun
 */
@ShellComponent
public class OperaShellCommands {

    private static final Logger logger = LoggerFactory.getLogger(OperaShellCommands.class);

    private final AgentPlatform agentPlatform;
    private final OperaSceneStages sceneStages;
    private final OperaProductionStages productionStages;

    // Hold state between manual commands
    private Opera currentOpera;
    private OperaProductionStages.ProductionState currentProductionState;

    public OperaShellCommands(
            AgentPlatform agentPlatform,
            OperaSceneStages sceneStages,
            OperaProductionStages productionStages
    ) {
        this.agentPlatform = agentPlatform;
        this.sceneStages = sceneStages;
        this.productionStages = productionStages;
    }

    // ========================================================================
    // MANUAL MODE COMMANDS
    // ========================================================================

    @ShellMethod(key = "generate-opera", value = "Generate an opera using manual orchestration (code controls the workflow)")
    public String generateOpera(
            @ShellOption(value = {"-t", "--title"}, defaultValue = ShellOption.NULL, help = "Opera title (auto-generated if not provided)") String title,
            @ShellOption(value = {"-s", "--scenes"}, defaultValue = "3", help = "Number of scenes to generate") int numberOfScenes,
            @ShellOption(value = {"-p", "--premise"}, defaultValue = ShellOption.NULL, help = "Custom premise (uses default if not provided)") String premise
    ) {
        logger.info("═".repeat(60));
        logger.info("  MANUAL MODE - Generating Opera");
        logger.info("═".repeat(60));

        if (premise == null || premise.isBlank()) {
            premise = Conversation.defaultPremise();
        }

        // Generate title if not provided
        if (title == null || title.isBlank()) {
            title = sceneStages.generateTitle(premise, null);
        }

        // Initialize scene generation
        var request = new OperaSceneStages.SceneGenerationRequest(title, premise, numberOfScenes);
        var state = sceneStages.initializeGeneration(request);

        // Generate each scene (manual loop)
        while (!state.isComplete()) {
            state = sceneStages.writeNextScene(state, null);
        }

        // Build the opera
        currentOpera = sceneStages.buildOpera(state);

        logger.info("═".repeat(60));
        logger.info("  GENERATION COMPLETE");
        logger.info("═".repeat(60));

        return formatOperaSummary(currentOpera);
    }

    @ShellMethod(key = "run-production", value = "Run the production workflow for the current opera")
    public String runProduction() {
        if (currentOpera == null) {
            return "No opera generated. Run 'generate-opera' first.";
        }

        logger.info("═".repeat(60));
        logger.info("  MANUAL MODE - Running Production");
        logger.info("═".repeat(60));

        currentProductionState = productionStages.initializeProduction(currentOpera);
        currentProductionState = productionStages.completeProduction(currentProductionState);

        logger.info("═".repeat(60));
        logger.info("  PRODUCTION COMPLETE");
        logger.info("═".repeat(60));

        return productionStages.getStatus(currentProductionState);
    }

    @ShellMethod(key = "full-manual", value = "Generate and produce an opera in manual mode (all steps)")
    public String fullManual(
            @ShellOption(value = {"-t", "--title"}, defaultValue = ShellOption.NULL, help = "Opera title") String title,
            @ShellOption(value = {"-s", "--scenes"}, defaultValue = "3", help = "Number of scenes") int numberOfScenes
    ) {
        String genResult = generateOpera(title, numberOfScenes, null);
        String prodResult = runProduction();
        return genResult + "\n" + prodResult;
    }

    // ========================================================================
    // AGENTIC MODE COMMANDS
    // ========================================================================

    @ShellMethod(key = "create-opera", value = "Create an opera using agentic orchestration (GOAP planner decides)")
    public String createOperaAgentic(
            @ShellOption(value = {"-t", "--title"}, defaultValue = ShellOption.NULL, help = "Opera title (auto-generated if not provided)") String title,
            @ShellOption(value = {"-s", "--scenes"}, defaultValue = "3", help = "Number of scenes") int numberOfScenes,
            @ShellOption(value = {"-p", "--premise"}, defaultValue = ShellOption.NULL, help = "Custom premise") String premise,
            @ShellOption(value = {"-v", "--verbose"}, defaultValue = "false", help = "Show detailed prompts") boolean verbose
    ) {
        logger.info("═".repeat(60));
        logger.info("  AGENTIC MODE - GOAP Planner Takes Control");
        logger.info("═".repeat(60));

        if (premise == null || premise.isBlank()) {
            premise = Conversation.defaultPremise();
        }

        if (title == null || title.isBlank()) {
            title = "Opera Generation Request";
        }

        // Create the initial request that goes on the blackboard
        var request = new OperaSceneStages.SceneGenerationRequest(title, premise, numberOfScenes);

        try {
            // Build the agent invocation
            // Note: verbose option would be configured via ProcessOptions if needed
            var invocation = AgentInvocation.builder(agentPlatform)
                    .build(OperaProductionStages.ProductionState.class);

            // Let the GOAP planner figure out the sequence
            logger.info("Request: Create {}-scene opera with premise...", numberOfScenes);
            logger.info("Handing control to GOAP planner...");

            var result = invocation.invoke(request);

            currentProductionState = result;
            currentOpera = result.opera();

            logger.info("═".repeat(60));
            logger.info("  AGENTIC GENERATION COMPLETE");
            logger.info("═".repeat(60));

            return productionStages.getStatus(result);

        } catch (Exception e) {
            logger.error("Agentic execution failed", e);
            return "Agentic execution failed: " + e.getMessage();
        }
    }

    @ShellMethod(key = "create-opera-simple", value = "Create an opera using autonomous agent with natural language request")
    public String createOperaSimple(
            @ShellOption(value = {"-r", "--request"}, defaultValue = "Create a 2-scene opera about love and technology", help = "Natural language request") String request,
            @ShellOption(value = {"-v", "--verbose"}, defaultValue = "false", help = "Show detailed output") boolean verbose
    ) {
        logger.info("═".repeat(60));
        logger.info("  AGENTIC MODE - Natural Language Request");
        logger.info("═".repeat(60));
        logger.info("Request: {}", request);

        try {
            // Build the agent invocation
            var invocation = AgentInvocation.builder(agentPlatform)
                    .build(OperaProductionStages.ProductionState.class);

            // Parse scenes from request or default to 2
            int scenes = 2;
            if (request.matches(".*\\d+-scene.*")) {
                scenes = Integer.parseInt(request.replaceAll(".*?(\\d+)-scene.*", "$1"));
            }

            var sceneRequest = new OperaSceneStages.SceneGenerationRequest(
                    "Autonomous Opera",
                    Conversation.defaultPremise() + "\n\nUser request: " + request,
                    scenes
            );

            var result = invocation.invoke(sceneRequest);
            currentProductionState = result;
            currentOpera = result.opera();

            logger.info("═".repeat(60));
            logger.info("  COMPLETE");
            logger.info("═".repeat(60));

            return productionStages.getStatus(result);

        } catch (Exception e) {
            logger.error("Execution failed", e);
            return "Failed: " + e.getMessage();
        }
    }

    // ========================================================================
    // UTILITY COMMANDS
    // ========================================================================

    @ShellMethod(key = "status", value = "Show the current opera status")
    public String status() {
        if (currentOpera == null) {
            return "No opera currently loaded. Use 'generate-opera' or 'create-opera' first.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatOperaSummary(currentOpera));

        if (currentProductionState != null) {
            sb.append("\n").append(productionStages.getStatus(currentProductionState));
        }

        return sb.toString();
    }

    @ShellMethod(key = "default-premise", value = "Show the default opera premise")
    public String showDefaultPremise() {
        return "Default Opera Premise:\n" + "-".repeat(40) + "\n" + Conversation.defaultPremise();
    }

    @ShellMethod(key = "help-opera", value = "Show help for opera generation commands")
    public String helpOpera() {
        return """
                ==============================================================
                  EMBABEL OPERA GENERATOR - Command Reference
                ==============================================================

                MANUAL MODE (you control the workflow):
                ------------------------------------------------------------
                  generate-opera   Generate scenes with alternating models
                                   Options: -t <title> -s <scenes> -p <premise>

                  run-production   Run production workflow (save, images, etc.)

                  full-manual      Generate and produce in one command

                AGENTIC MODE (GOAP planner decides):
                ------------------------------------------------------------
                  create-opera         Structured agentic generation
                                       Options: -t <title> -s <scenes> -v

                  create-opera-simple  Natural language request
                                       Options: -r <request> -v

                UTILITIES:
                ------------------------------------------------------------
                  status           Show current opera and production status
                  default-premise  Show the default opera premise
                  help-opera       Show this help message

                ==============================================================
                """;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private String formatOperaSummary(Opera opera) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nOpera: ").append(opera.title()).append("\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append("Scenes: ").append(opera.scenes().size()).append("\n\n");

        for (Opera.Scene scene : opera.scenes()) {
            sb.append("  Scene ").append(scene.number())
                    .append(": ").append(scene.title())
                    .append(" (by ").append(scene.author()).append(")\n");
        }

        return sb.toString();
    }
}
