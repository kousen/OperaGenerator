package com.kousenit;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.kousenit.exception.ImageGenerationException;
import com.kousenit.util.SemaphorePermit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Generates images for opera scenes using Google's Gemini Nano Banana (gemini-3-pro-image-preview).
 * Uses virtual threads and rate limiting to prevent API throttling.
 */
public class GeminiImageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(GeminiImageGenerator.class);
    public static String RESOURCE_PATH = "src/main/resources";

    private static final String MODEL_NAME = "gemini-3-pro-image-preview";

    // Rate limiting configuration (configurable via system properties or environment variables)
    private static final int MAX_CONCURRENT_REQUESTS = getMaxConcurrentRequests();
    private static final Duration DELAY_BETWEEN_REQUESTS = getDelayBetweenRequests();

    private static int getMaxConcurrentRequests() {
        // Check system property first, then environment variable, then default
        String prop = System.getProperty("opera.image.maxConcurrent");
        if (prop == null) {
            prop = System.getenv("OPERA_IMAGE_MAX_CONCURRENT");
        }
        if (prop != null) {
            try {
                int value = Integer.parseInt(prop);
                logger.info("Using MAX_CONCURRENT_REQUESTS={} from configuration", value);
                return value;
            } catch (NumberFormatException e) {
                logger.warn("Invalid MAX_CONCURRENT_REQUESTS value: {}, using default", prop);
            }
        }

        // Default conservative value - can be increased based on rate limit testing
        logger.info("Using default MAX_CONCURRENT_REQUESTS=2 (conservative). " +
                   "Set OPERA_IMAGE_MAX_CONCURRENT env var or opera.image.maxConcurrent system property to override.");
        return 2;
    }

    private static Duration getDelayBetweenRequests() {
        // Check system property first, then environment variable, then default
        String prop = System.getProperty("opera.image.delayMs");
        if (prop == null) {
            prop = System.getenv("OPERA_IMAGE_DELAY_MS");
        }
        if (prop != null) {
            try {
                long ms = Long.parseLong(prop);
                logger.info("Using DELAY_BETWEEN_REQUESTS={}ms from configuration", ms);
                return Duration.ofMillis(ms);
            } catch (NumberFormatException e) {
                logger.warn("Invalid DELAY_MS value: {}, using default", prop);
            }
        }

        // Default 1 second delay - can be reduced based on rate limit testing
        logger.info("Using default DELAY_BETWEEN_REQUESTS=1000ms. " +
                   "Set OPERA_IMAGE_DELAY_MS env var or opera.image.delayMs system property to override.");
        return Duration.ofSeconds(1);
    }

    public static void generateImages(Opera opera) {
        generateImages(opera, MAX_CONCURRENT_REQUESTS, DELAY_BETWEEN_REQUESTS);
    }

    public static void generateImages(Opera opera, int maxConcurrent, Duration delayBetween) {
        // Use semaphore to limit concurrent requests
        Semaphore rateLimiter = new Semaphore(maxConcurrent);

        List<ImageGenerationException> failures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = opera.scenes().stream()
                    .map(scene -> CompletableFuture.runAsync(() -> {
                        try {
                            generateSingleImageWithRateLimit(scene, rateLimiter, delayBetween);
                        } catch (Exception e) {
                            logger.error("Failed to generate image for scene {}: {}", scene.number(), e.getMessage());
                            failures.add(new ImageGenerationException(
                                "Image generation failed for scene " + scene.number(), scene.number(), e));
                        }
                    }, executor))
                    .toList();

            // Wait for all image generations to complete with timeout
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .orTimeout(10, TimeUnit.MINUTES)
                    .join();
            } catch (CompletionException e) {
                logger.error("Image generation timed out or failed: {}", e.getMessage());
            }

            logger.info("All image generation tasks completed. Failures: {}", failures.size());
            if (!failures.isEmpty()) {
                logger.warn("Failed to generate {} images", failures.size());
                failures.forEach(f -> logger.warn("  Scene {}: {}", f.getSceneNumber(), f.getMessage()));
            }
        }
    }

    private static void generateSingleImageWithRateLimit(Opera.Scene scene,
                                                         Semaphore rateLimiter,
                                                         Duration delayBetween) throws ImageGenerationException {
        try (var permit = new SemaphorePermit(rateLimiter)) {
            // Add delay to throttle requests
            if (!delayBetween.isZero()) {
                Thread.sleep(delayBetween.toMillis());
            }

            generateSingleImage(scene);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Image generation interrupted for Scene {}", scene.number());
            throw new ImageGenerationException(
                "Image generation interrupted for scene " + scene.number(), scene.number(), e);
        } catch (Exception e) {
            logger.error("Failed to generate image for Scene {}: {}", scene.number(), e.getMessage());
            throw new ImageGenerationException(
                "Failed to generate image for scene " + scene.number(), scene.number(), e);
        }
    }

    private static void generateSingleImage(Opera.Scene scene) throws ImageGenerationException {
        String prompt = String.format("""
                Create a cinematic concept illustration for Scene %d: %s.
                Scene description: %s
                Render an ultra-realistic modern opera production with dramatic stage lighting,
                rich costuming, expressive posing, and atmospheric depth. Emphasize emotional
                intensity, nuanced facial expressions, and layered set design. Use a 16:9 wide
                composition suitable for a stage backdrop. Avoid cartoon, caricature, flat shading,
                or comic styles.
                """,
                scene.number(),
                scene.title(),
                scene.content().substring(0, Math.min(600, scene.content().length()))
        );

        try (Client client = Client.builder()
                .apiKey(ApiKeys.GOOGLE_API_KEY)
                .build()) {
            // Configure to generate images
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("IMAGE")
                    .build();

            logger.info("Generating image for Scene {}: {}", scene.number(), scene.title());

            // Generate the image
            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME,
                    prompt,
                    config);

            // Extract and save image from response
            boolean imageSaved = false;
            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] imageData = blob.data().get();
                        Path outputPath = Path.of(RESOURCE_PATH, scene.getImageFileName());

                        // Ensure parent directory exists
                        Files.createDirectories(outputPath.getParent());

                        Files.write(outputPath, imageData);
                        logger.info("Generated and saved image: {}", outputPath);
                        imageSaved = true;
                        break;
                    }
                }
            }

            if (!imageSaved) {
                logger.error("No image data found in response for Scene {}", scene.number());
                throw new ImageGenerationException(
                    "No image data found in response for scene " + scene.number(), scene.number());
            }

        } catch (IOException e) {
            logger.error("Error generating or saving image for Scene {}: {}", scene.number(), e.getMessage());
            throw new ImageGenerationException(
                "Error processing image for scene " + scene.number(), scene.number(), e);
        } catch (Exception e) {
            logger.error("Unexpected error generating image for Scene {}: {}", scene.number(), e.getMessage());
            throw new ImageGenerationException(
                "Unexpected error for scene " + scene.number(), scene.number(), e);
        }
    }
}
