package com.kousenit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Individual pipeline steps that can be invoked independently by Claude Code team agents.
 * Each step reads/writes through the filesystem, using JSON-serialized Opera as the shared state.
 *
 * <p>Usage from Gradle:
 * <pre>
 *   ./gradlew generateScenes                          # Step 1: Generate scenes
 *   ./gradlew generateScenes -PsceneCount=3           # With custom scene count
 *   ./gradlew saveLibretto -PoperaJson=path/to/opera.json  # Step 2: Save libretto
 *   ./gradlew generateImages -PoperaJson=path/to/opera.json # Step 3: Images (parallel)
 *   ./gradlew generateNarration -PoperaJson=path/to/opera.json # Step 4: Narration
 *   ./gradlew generateCritique -PoperaJson=path/to/opera.json  # Step 5: Critique
 * </pre>
 */
public class PipelineSteps {

    public static void main(String[] args) {
        String step = System.getProperty("pipeline.step", "");
        switch (step) {
            case "generateScenes" -> generateScenes(args);
            case "generateImages" -> generateImages(args);
            case "generateNarration" -> generateNarration(args);
            case "generateCritique" -> generateCritique(args);
            default -> {
                System.err.println("Unknown pipeline step: " + step);
                System.err.println("Valid steps: generateScenes, generateImages, generateNarration, generateCritique");
                System.exit(1);
            }
        }
    }

    /**
     * Step 1: Generate opera scenes with alternating AI models.
     * Outputs: opera.json in the opera's resource directory.
     */
    public static void generateScenes(String[] args) {
        try {
            String title = args.length > 0 && !args[0].isBlank() ? args[0] : null;
            int numberOfScenes = args.length > 1 ? Integer.parseInt(args[1]) : 5;

            System.out.println("=== STEP 1: Scene Generation ===");
            System.out.printf("Generating %d scenes with GPT-5.2 and Claude Opus 4.5...%n", numberOfScenes);

            Conversation conversation = new Conversation();
            Opera opera = conversation.generateOpera(title, numberOfScenes);

            System.out.printf("Generated opera: \"%s\" with %d scenes%n", opera.title(), opera.scenes().size());

            // Save libretto and scene files
            Path librettoPath = LibrettoWriter.saveCompleteOpera(opera);
            Path operaDir = librettoPath.getParent();

            // Serialize Opera to JSON for downstream agents
            Path jsonPath = operaDir.resolve("opera.json");
            OperaSerializer.save(opera, jsonPath);
            System.out.println("Opera JSON saved to: " + jsonPath);

            System.out.println("=== Scene generation complete ===");
            System.out.println("Opera directory: " + operaDir);
            System.out.println("Pass -PoperaJson=" + jsonPath + " to downstream steps");

        } catch (Exception e) {
            System.err.println("Scene generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Step 2: Generate illustrations for each scene using Gemini Nano Banana.
     * Reads opera.json, generates images in parallel with virtual threads.
     */
    public static void generateImages(String[] args) {
        try {
            Path jsonPath = resolveOperaJson(args);
            Opera opera = OperaSerializer.load(jsonPath);
            Path operaDir = jsonPath.getParent();

            System.out.println("=== STEP 2: Image Generation ===");
            System.out.printf("Generating illustrations for \"%s\" (%d scenes)...%n",
                    opera.title(), opera.scenes().size());

            // Point image generator at the opera directory
            GeminiImageGenerator.RESOURCE_PATH = operaDir.toString();
            GeminiImageGenerator.generateImages(opera);

            System.out.println("=== Image generation complete ===");

        } catch (Exception e) {
            System.err.println("Image generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Step 3: Generate audio narration using ElevenLabs.
     * Reads opera.json, generates introduction and per-scene narration.
     */
    public static void generateNarration(String[] args) {
        try {
            Path jsonPath = resolveOperaJson(args);
            Opera opera = OperaSerializer.load(jsonPath);
            Path operaDir = jsonPath.getParent();

            System.out.println("=== STEP 3: Narration Generation ===");
            System.out.printf("Generating narration for \"%s\"...%n", opera.title());

            NarratorVoice narrator = new NarratorVoice();

            // Generate dramatic introduction
            Path introAudio = narrator.generateOperaIntroduction(opera, operaDir);
            System.out.println("Introduction narration: " + introAudio.getFileName());

            // Generate narration for each scene
            for (Opera.Scene scene : opera.scenes()) {
                Path sceneAudio = narrator.generateSceneNarration(scene, operaDir);
                if (sceneAudio != null) {
                    System.out.printf("Scene %d narration: %s%n", scene.number(), sceneAudio.getFileName());
                }
            }

            System.out.println("=== Narration generation complete ===");

        } catch (Exception e) {
            System.err.println("Narration generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Step 4: Generate critical review using Gemini 3 Pro.
     * Reads opera.json, generates a newspaper-style review.
     */
    public static void generateCritique(String[] args) {
        try {
            Path jsonPath = resolveOperaJson(args);
            Opera opera = OperaSerializer.load(jsonPath);
            Path operaDir = jsonPath.getParent();

            System.out.println("=== STEP 4: Critique Generation ===");
            System.out.printf("Generating critical review of \"%s\"...%n", opera.title());

            OperaCritic critic = new OperaCritic();
            critic.reviewAndSave(operaDir, opera.title());

            System.out.println("=== Critique generation complete ===");

        } catch (Exception e) {
            System.err.println("Critique generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path resolveOperaJson(String[] args) {
        if (args.length == 0 || args[0].isBlank()) {
            // Try to find the most recent opera.json in resources
            Path resourcesDir = Paths.get("src/main/resources");
            try (var dirs = Files.list(resourcesDir)) {
                return dirs.filter(Files::isDirectory)
                        .map(dir -> dir.resolve("opera.json"))
                        .filter(Files::exists)
                        .sorted((a, b) -> {
                            try {
                                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No opera.json found. Run generateScenes first, or pass the path as an argument."));
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not search for opera.json: " + e.getMessage());
            }
        }
        Path path = Path.of(args[0]);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Opera JSON not found: " + path);
        }
        return path;
    }
}
