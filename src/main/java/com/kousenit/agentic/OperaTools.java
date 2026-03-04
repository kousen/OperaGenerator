package com.kousenit.agentic;

import com.kousenit.*;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools available to the Opera Supervisor Agent.
 * These are utility functions that don't require LLM reasoning.
 */
public class OperaTools {
    private static final Logger logger = LoggerFactory.getLogger(OperaTools.class);

    private Opera currentOpera;
    private Path operaDirectory;
    private final List<SceneData> collectedScenes = new ArrayList<>();

    /**
     * Internal class to hold scene data before building the Opera.
     */
    private record SceneData(int number, String content, String author) {}

    public void setCurrentOpera(Opera opera) {
        this.currentOpera = opera;
    }

    public Opera getCurrentOpera() {
        return currentOpera;
    }

    public Path getOperaDirectory() {
        return operaDirectory;
    }

    @Tool("Save the complete opera libretto to a formatted markdown file")
    public String saveLibretto() {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        try {
            Path librettoPath = LibrettoWriter.saveCompleteOpera(currentOpera);
            this.operaDirectory = librettoPath.getParent();
            logger.info("Saved libretto to: {}", librettoPath);
            return "Libretto saved successfully to: " + librettoPath.getFileName();
        } catch (IOException e) {
            logger.error("Failed to save libretto", e);
            return "Error saving libretto: " + e.getMessage();
        }
    }

    @Tool("Generate illustrations for all scenes using Google Gemini Nano Banana. This runs in parallel for efficiency.")
    public String generateAllImages() {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        if (operaDirectory == null) {
            return "Error: Please save the libretto first to establish the output directory.";
        }

        try {
            // Set the resource path for GeminiImageGenerator
            GeminiImageGenerator.RESOURCE_PATH = operaDirectory.toString();

            logger.info("Starting parallel image generation for {} scenes", currentOpera.scenes().size());
            GeminiImageGenerator.generateImages(currentOpera);

            return "Successfully generated " + currentOpera.scenes().size() + " illustrations.";
        } catch (Exception e) {
            logger.error("Failed to generate images", e);
            return "Error generating images: " + e.getMessage();
        }
    }

    @Tool("Generate a single illustration for a specific scene number")
    public String generateImageForScene(int sceneNumber) {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        if (operaDirectory == null) {
            return "Error: Please save the libretto first.";
        }

        Opera.Scene scene = currentOpera.scenes().stream()
                .filter(s -> s.number() == sceneNumber)
                .findFirst()
                .orElse(null);

        if (scene == null) {
            return "Error: Scene " + sceneNumber + " not found.";
        }

        try {
            GeminiImageGenerator.RESOURCE_PATH = operaDirectory.toString();
            GeminiImageGenerator.generateImages(new Opera(
                    currentOpera.title(),
                    currentOpera.premise(),
                    List.of(scene)
            ));
            return "Generated illustration for Scene " + sceneNumber;
        } catch (Exception e) {
            logger.error("Failed to generate image for scene {}", sceneNumber, e);
            return "Error generating image: " + e.getMessage();
        }
    }

    @Tool("Generate audio narration for the opera introduction and scene stage directions. Requires ELEVENLABS_API_KEY.")
    public String generateNarration() {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        if (operaDirectory == null) {
            return "Error: Please save the libretto first.";
        }
        if (System.getenv("ELEVENLABS_API_KEY") == null) {
            return "Skipped: ELEVENLABS_API_KEY not configured.";
        }

        try {
            NarratorVoice narrator = new NarratorVoice();

            // Generate introduction
            Path introAudio = narrator.generateOperaIntroduction(currentOpera, operaDirectory);
            logger.info("Generated introduction narration: {}", introAudio.getFileName());

            // Generate scene narrations
            int narratedScenes = 0;
            for (Opera.Scene scene : currentOpera.scenes()) {
                Path sceneAudio = narrator.generateSceneNarration(scene, operaDirectory);
                if (sceneAudio != null) {
                    narratedScenes++;
                }
            }

            return "Generated audio narration: introduction + " + narratedScenes + " scene narrations.";
        } catch (Exception e) {
            logger.error("Failed to generate narration", e);
            return "Error generating narration: " + e.getMessage();
        }
    }

    @Tool("Generate a critical review of the opera using Google Gemini. Requires GOOGLEAI_API_KEY.")
    public String generateCritique() {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        if (operaDirectory == null) {
            return "Error: Please save the libretto first.";
        }
        if (System.getenv("GOOGLEAI_API_KEY") == null) {
            return "Skipped: GOOGLEAI_API_KEY not configured.";
        }

        try {
            OperaCritic critic = new OperaCritic();
            critic.reviewAndSave(operaDirectory, currentOpera.title());
            logger.info("Generated critique for: {}", currentOpera.title());
            return "Critical review generated and saved.";
        } catch (Exception e) {
            logger.error("Failed to generate critique", e);
            return "Error generating critique: " + e.getMessage();
        }
    }

    @Tool("Prepare export packages for external tools like Suno AI and NotebookLM")
    public String prepareExports() {
        if (currentOpera == null) {
            return "Error: No opera has been generated yet.";
        }
        if (operaDirectory == null) {
            return "Error: Please save the libretto first.";
        }

        try {
            ExternalToolsPreparer.generateSunoPrompts(currentOpera, operaDirectory);
            ExternalToolsPreparer.prepareNotebookLMPackage(currentOpera, operaDirectory);
            return "Export packages prepared for Suno AI and NotebookLM.";
        } catch (IOException e) {
            logger.error("Failed to prepare exports", e);
            return "Error preparing exports: " + e.getMessage();
        }
    }

    @Tool("Get the current status of opera generation")
    public String getStatus() {
        if (currentOpera == null) {
            return "No opera generated yet.";
        }
        return String.format("""
                Opera: %s
                Scenes: %d
                Directory: %s
                """,
                currentOpera.title(),
                currentOpera.scenes().size(),
                operaDirectory != null ? operaDirectory.toString() : "Not saved yet"
        );
    }

    @Tool("Register a scene that was generated by a scene writer. Call this after each scene is written.")
    public String registerScene(
            @P("The scene number (1, 2, 3...)") int sceneNumber,
            @P("The full scene content as returned by the scene writer") String sceneContent,
            @P("The name of the model that wrote this scene (e.g., 'GPT-5.2' or 'Claude Opus 4.6')") String authorModel) {

        // Check for duplicate
        boolean exists = collectedScenes.stream()
                .anyMatch(s -> s.number() == sceneNumber);
        if (exists) {
            return "Scene " + sceneNumber + " already registered. Skipping duplicate.";
        }

        collectedScenes.add(new SceneData(sceneNumber, sceneContent, authorModel));
        logger.info("Registered Scene {} by {}", sceneNumber, authorModel);
        return "Scene " + sceneNumber + " registered successfully. Total scenes collected: " + collectedScenes.size();
    }

    @Tool("Build the Opera object from all registered scenes. Call this AFTER all scenes have been registered with registerScene.")
    public String buildOpera(
            @P("The title of the opera") String title,
            @P("The premise or setting of the opera") String premise) {

        if (collectedScenes.isEmpty()) {
            return "Error: No scenes have been registered. Use registerScene() for each scene first.";
        }

        // Sort scenes by number and parse them
        List<Opera.Scene> scenes = collectedScenes.stream()
                .sorted((a, b) -> Integer.compare(a.number(), b.number()))
                .map(data -> Opera.Scene.parse(data.number(), data.content(), data.author()))
                .toList();

        // Create the Opera
        this.currentOpera = new Opera(title, premise, scenes);
        logger.info("Built Opera '{}' with {} scenes", title, scenes.size());

        // Clear collected scenes for potential reuse
        collectedScenes.clear();

        return String.format("Opera '%s' built successfully with %d scenes. Ready for production workflow.",
                title, scenes.size());
    }
}
