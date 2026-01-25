package com.kousenit.embabel;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.kousenit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Embabel agent for handling opera production tasks.
 * <p>
 * This agent wraps the existing utility classes:
 * - LibrettoWriter: Save formatted markdown libretto
 * - GeminiImageGenerator: Generate illustrations with Nano Banana
 * - NarratorVoice: Generate ElevenLabs audio narration
 * - OperaCritic: Generate Gemini AI critique
 * - ExternalToolsPreparer: Create Suno and NotebookLM packages
 */
@Agent(description = "Handles opera production: saving, illustrations, narration, critique, and exports")
@Component
public class OperaProductionStages {

    private static final Logger logger = LoggerFactory.getLogger(OperaProductionStages.class);

    @Value("${opera.output.base-directory:src/main/resources}")
    private String baseDirectory;

    /**
     * Domain record representing the production state for an opera.
     */
    public record ProductionState(
            Opera opera,
            Path operaDirectory,
            boolean librettoSaved,
            boolean imagesGenerated,
            boolean narrationGenerated,
            boolean critiqueGenerated,
            boolean exportsReady
    ) {
        public static ProductionState forOpera(Opera opera) {
            return new ProductionState(opera, null, false, false, false, false, false);
        }

        public ProductionState withDirectory(Path directory) {
            return new ProductionState(opera, directory, librettoSaved, imagesGenerated,
                    narrationGenerated, critiqueGenerated, exportsReady);
        }

        public ProductionState withLibrettoSaved() {
            return new ProductionState(opera, operaDirectory, true, imagesGenerated,
                    narrationGenerated, critiqueGenerated, exportsReady);
        }

        public ProductionState withImagesGenerated() {
            return new ProductionState(opera, operaDirectory, librettoSaved, true,
                    narrationGenerated, critiqueGenerated, exportsReady);
        }

        public ProductionState withNarrationGenerated() {
            return new ProductionState(opera, operaDirectory, librettoSaved, imagesGenerated,
                    true, critiqueGenerated, exportsReady);
        }

        public ProductionState withCritiqueGenerated() {
            return new ProductionState(opera, operaDirectory, librettoSaved, imagesGenerated,
                    narrationGenerated, true, exportsReady);
        }

        public ProductionState withExportsReady() {
            return new ProductionState(opera, operaDirectory, librettoSaved, imagesGenerated,
                    narrationGenerated, critiqueGenerated, true);
        }

        public boolean isComplete() {
            return librettoSaved && imagesGenerated && narrationGenerated
                    && critiqueGenerated && exportsReady;
        }
    }

    /**
     * Initialize production state from an Opera.
     */
    @Action(description = "Initialize production state for an opera")
    public ProductionState initializeProduction(Opera opera) {
        logger.info("Initializing production for: {}", opera.title());
        return ProductionState.forOpera(opera);
    }

    /**
     * Save the opera libretto to formatted markdown.
     * This must be called first as it establishes the output directory.
     */
    @Action(description = "Save the opera libretto to a formatted markdown file")
    public ProductionState saveLibretto(ProductionState state) {
        if (state.librettoSaved()) {
            logger.info("Libretto already saved, skipping");
            return state;
        }

        try {
            Path librettoPath = LibrettoWriter.saveCompleteOpera(state.opera());
            Path operaDirectory = librettoPath.getParent();
            logger.info("Saved libretto to: {}", librettoPath);
            return state.withDirectory(operaDirectory).withLibrettoSaved();
        } catch (IOException e) {
            logger.error("Failed to save libretto", e);
            throw new RuntimeException("Failed to save libretto: " + e.getMessage(), e);
        }
    }

    /**
     * Generate illustrations for all scenes using Gemini Nano Banana.
     * Requires libretto to be saved first (to establish output directory).
     */
    @Action(description = "Generate illustrations for all scenes using Google Gemini")
    public ProductionState generateImages(ProductionState state) {
        if (!state.librettoSaved()) {
            throw new IllegalStateException("Must save libretto before generating images");
        }
        if (state.imagesGenerated()) {
            logger.info("Images already generated, skipping");
            return state;
        }

        try {
            GeminiImageGenerator.RESOURCE_PATH = state.operaDirectory().toString();
            logger.info("Generating illustrations for {} scenes...", state.opera().scenes().size());
            GeminiImageGenerator.generateImages(state.opera());
            logger.info("Successfully generated {} illustrations", state.opera().scenes().size());
            return state.withImagesGenerated();
        } catch (Exception e) {
            logger.error("Failed to generate images", e);
            throw new RuntimeException("Failed to generate images: " + e.getMessage(), e);
        }
    }

    /**
     * Generate audio narration using ElevenLabs.
     * Requires ELEVENLABS_API_KEY environment variable.
     */
    @Action(description = "Generate audio narration using ElevenLabs text-to-speech")
    public ProductionState generateNarration(ProductionState state) {
        if (!state.librettoSaved()) {
            throw new IllegalStateException("Must save libretto before generating narration");
        }
        if (state.narrationGenerated()) {
            logger.info("Narration already generated, skipping");
            return state;
        }

        String elevenLabsKey = System.getenv("ELEVENLABS_API_KEY");
        if (elevenLabsKey == null || elevenLabsKey.isBlank()) {
            logger.warn("ELEVENLABS_API_KEY not set, skipping narration generation");
            return state.withNarrationGenerated(); // Mark as done (skipped)
        }

        try {
            NarratorVoice narrator = new NarratorVoice();

            // Generate introduction
            Path introAudio = narrator.generateOperaIntroduction(state.opera(), state.operaDirectory());
            logger.info("Generated introduction narration: {}", introAudio.getFileName());

            // Generate scene narrations
            int narratedScenes = 0;
            for (Opera.Scene scene : state.opera().scenes()) {
                Path sceneAudio = narrator.generateSceneNarration(scene, state.operaDirectory());
                if (sceneAudio != null) {
                    narratedScenes++;
                }
            }
            logger.info("Generated narration for {} scenes", narratedScenes);
            return state.withNarrationGenerated();
        } catch (Exception e) {
            logger.error("Failed to generate narration", e);
            throw new RuntimeException("Failed to generate narration: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a critical review using Google Gemini.
     * Requires GOOGLEAI_API_KEY environment variable.
     */
    @Action(description = "Generate a critical review of the opera using Gemini")
    public ProductionState generateCritique(ProductionState state) {
        if (!state.librettoSaved()) {
            throw new IllegalStateException("Must save libretto before generating critique");
        }
        if (state.critiqueGenerated()) {
            logger.info("Critique already generated, skipping");
            return state;
        }

        String googleKey = System.getenv("GOOGLEAI_API_KEY");
        if (googleKey == null || googleKey.isBlank()) {
            logger.warn("GOOGLEAI_API_KEY not set, skipping critique generation");
            return state.withCritiqueGenerated(); // Mark as done (skipped)
        }

        try {
            OperaCritic critic = new OperaCritic();
            critic.reviewAndSave(state.operaDirectory(), state.opera().title());
            logger.info("Generated critique for: {}", state.opera().title());
            return state.withCritiqueGenerated();
        } catch (Exception e) {
            logger.error("Failed to generate critique", e);
            throw new RuntimeException("Failed to generate critique: " + e.getMessage(), e);
        }
    }

    /**
     * Prepare export packages for external tools (Suno AI, NotebookLM).
     */
    @Action(description = "Prepare export packages for Suno AI and NotebookLM")
    public ProductionState prepareExports(ProductionState state) {
        if (!state.librettoSaved()) {
            throw new IllegalStateException("Must save libretto before preparing exports");
        }
        if (state.exportsReady()) {
            logger.info("Exports already prepared, skipping");
            return state;
        }

        try {
            ExternalToolsPreparer.generateSunoPrompts(state.opera(), state.operaDirectory());
            ExternalToolsPreparer.prepareNotebookLMPackage(state.opera(), state.operaDirectory());
            logger.info("Prepared export packages for Suno AI and NotebookLM");
            return state.withExportsReady();
        } catch (IOException e) {
            logger.error("Failed to prepare exports", e);
            throw new RuntimeException("Failed to prepare exports: " + e.getMessage(), e);
        }
    }

    /**
     * Run the complete production workflow.
     * This is the goal that GOAP planning aims to achieve.
     */
    @AchievesGoal(description = "Complete the full opera production workflow")
    @Action(description = "Execute the complete production workflow")
    public ProductionState completeProduction(ProductionState state) {
        logger.info("Running complete production workflow for: {}", state.opera().title());

        // Execute each step in sequence
        state = saveLibretto(state);
        state = generateImages(state);
        state = generateNarration(state);
        state = generateCritique(state);
        state = prepareExports(state);

        logger.info("Production complete for: {}", state.opera().title());
        return state;
    }

    /**
     * Get a status summary of the production state.
     */
    @Action(description = "Get the current production status")
    public String getStatus(ProductionState state) {
        return """
                Production Status for: %s
                ─────────────────────────────
                Directory: %s
                Libretto:  %s
                Images:    %s
                Narration: %s
                Critique:  %s
                Exports:   %s
                ─────────────────────────────
                Complete:  %s
                """.formatted(
                state.opera().title(),
                state.operaDirectory() != null ? state.operaDirectory() : "(not saved)",
                state.librettoSaved() ? "✓" : "○",
                state.imagesGenerated() ? "✓" : "○",
                state.narrationGenerated() ? "✓" : "○",
                state.critiqueGenerated() ? "✓" : "○",
                state.exportsReady() ? "✓" : "○",
                state.isComplete() ? "YES" : "NO"
        );
    }
}
