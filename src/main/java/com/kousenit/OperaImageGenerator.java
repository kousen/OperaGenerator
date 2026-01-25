package com.kousenit;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import com.kousenit.exception.ImageGenerationException;
import com.kousenit.util.SemaphorePermit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class OperaImageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OperaImageGenerator.class);
    static String RESOURCE_PATH = "production_runs"; // Package-private for testing

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
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")  // New streaming model with base64 output
                .timeout(Duration.ofMinutes(3))  // Increased timeout for gpt-image-1
                .build();

        // Use semaphore to limit concurrent requests
        Semaphore rateLimiter = new Semaphore(maxConcurrent);

        List<ImageGenerationException> failures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = opera.scenes().stream()
                    .map(scene -> CompletableFuture.runAsync(() -> {
                        try {
                            generateSingleImageWithRateLimit(model, scene, rateLimiter, delayBetween);
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


    private static void generateSingleImageWithRateLimit(OpenAiImageModel model, Opera.Scene scene,
                                                         Semaphore rateLimiter, Duration delayBetween) throws ImageGenerationException {
        try (var permit = new SemaphorePermit(rateLimiter)) {
            // Add delay to throttle requests
            if (!delayBetween.isZero()) {
                Thread.sleep(delayBetween.toMillis());
            }

            generateSingleImage(model, scene);

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

    private static void generateSingleImage(OpenAiImageModel model, Opera.Scene scene) throws ImageGenerationException {
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

        try {
            // Generate the image
            Response<Image> imageResponse = model.generate(prompt);

            // Debug: Check what type of data we received
            Image image = imageResponse.content();
            boolean hasBase64 = image.base64Data() != null && !image.base64Data().isEmpty();
            boolean hasUrl = image.url() != null;
            logger.debug("Scene {}: Has base64={}, Has URL={}",
                    scene.number(), hasBase64, hasUrl);

            // Save the image using our utility class
            Path savedPath = ImageSaver.saveImage(imageResponse, RESOURCE_PATH);

            if (savedPath != null) {
                // Rename the file to our desired filename
                Path newPath = savedPath.resolveSibling(scene.getImageFileName());
                Files.move(savedPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Generated and renamed image: {}", newPath);
            } else {
                logger.error("Failed to save image for Scene {}", scene.number());
                throw new ImageGenerationException(
                    "Failed to save image for scene " + scene.number(), scene.number());
            }
        } catch (IOException e) {
            logger.error("Error generating or renaming image for Scene {}: {}", scene.number(), e.getMessage());
            throw new ImageGenerationException(
                "Error processing image for scene " + scene.number(), scene.number(), e);
        }
    }


}
