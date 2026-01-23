package com.kousenit.agentic;

import com.kousenit.*;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tools available to the Opera Supervisor Agent.
 * These are utility functions that don't require LLM reasoning.
 */
public class OperaTools {
    private static final Logger logger = LoggerFactory.getLogger(OperaTools.class);

    private Opera currentOpera;
    private Path operaDirectory;

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
}
