package com.kousenit;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class ImageModelTest {

    private static final String TEST_PROMPT = """
            A steampunk robot playing a violin
            in a Victorian opera house,
            dramatic lighting, oil painting style
            """;

    @TempDir
    Path tempDir;


    @Test
    void testGptImage1ModelWithBase64Output() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .timeout(Duration.ofMinutes(3))  // Increase timeout for image generation
                .build();

        // When
        Response<Image> response = model.generate(TEST_PROMPT);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotNull();
        
        Image image = response.content();
        
        // Verify gpt-image-1 characteristics
        assertThat(image.base64Data())
                .as("gpt-image-1 should return base64 data")
                .isNotNull()
                .isNotEmpty();
        
        // gpt-image-1 might not return URLs (depending on implementation)
        System.out.printf("gpt-image-1 - Has base64: %s, Has URL: %s%n", 
                         image.base64Data() != null, image.url() != null);
        
        // Save the image first so we can see it even if other assertions fail
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        System.out.println("✅ gpt-image-1 test image saved to: " + savedPath);
        System.out.println("   Full path: " + savedPath.toAbsolutePath());
        
        // gpt-image-1 may not have revised prompt like DALL-E does
        if (image.revisedPrompt() != null) {
            System.out.println("Revised prompt: " + image.revisedPrompt());
        } else {
            System.out.println("No revised prompt returned (expected for gpt-image-1)");
        }
        
        // gpt-image-1 may not return token usage like DALL-E does
        if (response.tokenUsage() != null) {
            System.out.println("Token usage: " + response.tokenUsage());
        } else {
            System.out.println("No token usage returned (may be expected for gpt-image-1)");
        }
    }

    @Test
    void testDallE3ModelWithUrlOutput() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();

        // When
        Response<Image> response = model.generate(TEST_PROMPT);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotNull();
        
        Image image = response.content();
        
        // Verify DALL-E 3 characteristics
        assertThat(image.url())
                .as("DALL-E 3 should return a URL")
                .isNotNull();
        
        System.out.printf("DALL-E 3 - Has base64: %s, Has URL: %s%n", 
                         image.base64Data() != null && !image.base64Data().isEmpty(), 
                         image.url() != null);
        
        assertThat(image.revisedPrompt())
                .as("Should have a revised prompt")
                .isNotNull();
        
        assertThat(response.tokenUsage())
                .as("Should have token usage info")
                .isNotNull();
    }

    @Test
    void testImageSaverWithGptImage1() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .build();

        Response<Image> response = model.generate("A simple red circle on white background");

        // When
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());

        // Then
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        assertThat(savedPath.getFileName().toString())
                .as("Should be a PNG file")
                .endsWith(".png");
        
        System.out.println("✅ gpt-image-1 image saved to: " + savedPath.getFileName());
    }

    @Test
    void testImageSaverWithDallE3() {
        // Given
        var model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();

        Response<Image> response = model.generate("A simple blue square on white background");

        // When
        Path savedPath = ImageSaver.saveImage(response, tempDir.toString());

        // Then
        assertThat(savedPath)
                .as("Image should be saved successfully")
                .isNotNull()
                .exists()
                .isRegularFile();
        
        assertThat(savedPath.getFileName().toString())
                .as("Should be a PNG file")
                .endsWith(".png");
        
        System.out.println("✅ DALL-E 3 image saved to: " + savedPath.getFileName());
    }

    @Test
    void testModelComparison() {
        // This test compares the output characteristics of both models
        System.out.println("\n🔍 Model Comparison:");
        System.out.println("=" .repeat(50));
        
        // Test gpt-image-1
        var gptModel = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName("gpt-image-1")
                .build();
        
        Response<Image> gptResponse = gptModel.generate("A cat wearing a top hat");
        Image gptImage = gptResponse.content();
        
        // Test DALL-E 3
        var dalleModel = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(OpenAiImageModelName.DALL_E_3)
                .build();
        
        Response<Image> dalleResponse = dalleModel.generate("A cat wearing a top hat");
        Image dalleImage = dalleResponse.content();
        
        // Compare and report
        System.out.printf("gpt-image-1:  base64=%s, url=%s, tokens=%s%n",
                         gptImage.base64Data() != null && !gptImage.base64Data().isEmpty(),
                         gptImage.url() != null,
                         gptResponse.tokenUsage());
        
        System.out.printf("DALL-E 3:    base64=%s, url=%s, tokens=%s%n",
                         dalleImage.base64Data() != null && !dalleImage.base64Data().isEmpty(),
                         dalleImage.url() != null,
                         dalleResponse.tokenUsage());
        
        // Both should generate valid images
        assertThat(gptImage).isNotNull();
        assertThat(dalleImage).isNotNull();
    }
}