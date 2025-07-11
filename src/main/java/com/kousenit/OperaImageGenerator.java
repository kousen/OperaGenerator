package com.kousenit;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class OperaImageGenerator {

    static String RESOURCE_PATH = "src/main/resources"; // Package-private for testing
    
    // Rate limiting configuration
    private static final int MAX_CONCURRENT_REQUESTS = 2; // Conservative limit
    private static final Duration DELAY_BETWEEN_REQUESTS = Duration.ofSeconds(1); // Throttle requests

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
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = opera.scenes().stream()
                    .map(scene -> CompletableFuture.runAsync(() -> 
                            generateSingleImageWithRateLimit(model, scene, rateLimiter, delayBetween), executor))
                    .toList();
            
            // Wait for all image generations to complete
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            System.out.println("All image generation tasks completed.");
        }
    }
    

    private static void generateSingleImageWithRateLimit(OpenAiImageModel model, Opera.Scene scene, 
                                                        Semaphore rateLimiter, Duration delayBetween) {
        try {
            rateLimiter.acquire(); // Wait for permit
            
            // Add delay to throttle requests
            if (!delayBetween.isZero()) {
                Thread.sleep(delayBetween.toMillis());
            }
            
            generateSingleImage(model, scene);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Image generation interrupted for Scene " + scene.number());
        } finally {
            rateLimiter.release(); // Always release permit
        }
    }
    
    private static void generateSingleImage(OpenAiImageModel model, Opera.Scene scene) {
        String prompt = String.format(
                "Create an illustration for Scene %d: %s. The scene description: %s",
                scene.number(),
                scene.title(),
                scene.content().substring(0, Math.min(500, scene.content().length()))
        );
        
        try {
            // Generate the image
            Response<Image> imageResponse = model.generate(prompt);
            
            // Debug: Check what type of data we received
            Image image = imageResponse.content();
            boolean hasBase64 = image.base64Data() != null && !image.base64Data().isEmpty();
            boolean hasUrl = image.url() != null;
            System.out.printf("Scene %d: Has base64=%s, Has URL=%s%n", 
                             scene.number(), hasBase64, hasUrl);
            
            // Save the image using our utility class
            Path savedPath = ImageSaver.saveImage(imageResponse, RESOURCE_PATH);
            
            if (savedPath != null) {
                // Rename the file to our desired filename
                Path newPath = savedPath.resolveSibling(scene.getImageFileName());
                Files.move(savedPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Generated and renamed image: " + newPath);
            } else {
                System.err.println("Failed to save image for Scene " + scene.number());
            }
        } catch (IOException e) {
            System.err.println("Error generating or renaming image for Scene " + scene.number() + ": " + e.getMessage());
        }
    }
    

}
