package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class OperaGenerationIntegrationTest {

    @TempDir
    Path tempResourceDir;


    @Test
    void testCompleteOperaGeneration() {
        // Given
        String testTitle = "Test Opera for Integration";
        int numberOfScenes = 2; // Keep it small for testing
        
        System.out.println("🎭 Starting integration test for complete opera generation...");

        // When - Generate the opera
        Conversation conversation = new Conversation();
        Opera opera = conversation.generateOpera(testTitle, numberOfScenes);

        // Then - Verify opera structure
        assertThat(opera).isNotNull();
        assertThat(opera.title()).isEqualTo(testTitle);
        assertThat(opera.premise()).isNotEmpty();
        assertThat(opera.scenes()).hasSize(numberOfScenes);

        // Verify each scene has required fields
        for (int i = 0; i < numberOfScenes; i++) {
            Opera.Scene scene = opera.scenes().get(i);
            assertThat(scene.number()).isEqualTo(i + 1);
            assertThat(scene.title()).isNotEmpty();
            assertThat(scene.author()).isIn("GPT-4.1", "Claude Sonnet 4");
            assertThat(scene.content()).isNotEmpty();
        }

        // Verify alternating authors
        if (numberOfScenes >= 2) {
            assertThat(opera.scenes().get(0).author()).isEqualTo("GPT-4.1");
            assertThat(opera.scenes().get(1).author()).isEqualTo("Claude Sonnet 4");
        }

        System.out.printf("✅ Generated opera '%s' with %d scenes%n", opera.title(), opera.scenes().size());
    }

    @Test
    void testLibrettoWriting() throws IOException {
        // Given
        Opera.Scene scene1 = new Opera.Scene(1, "Opening", "GPT-4.1", "The curtain rises...");
        Opera.Scene scene2 = new Opera.Scene(2, "Encounter", "Claude Sonnet 4", "The characters meet...");
        Opera testOpera = new Opera("Test Libretto", "A test premise", List.of(scene1, scene2));

        // When - Save libretto
        String originalResourcePath = getResourcePath();
        setResourcePath(tempResourceDir.toString());
        
        try {
            Path librettoPath = LibrettoWriter.saveLibretto(testOpera);

            // Then - Verify libretto file
            assertThat(librettoPath).exists().isRegularFile();
            assertThat(librettoPath.getFileName().toString()).isEqualTo("test_libretto_libretto.md");

            String content = Files.readString(librettoPath);
            assertThat(content)
                    .contains("# Test Libretto")
                    .contains("## Premise")
                    .contains("A test premise")
                    .contains("### Scene 1: Opening")
                    .contains("> **Author: GPT-4.1**")
                    .contains("The curtain rises...")
                    .contains("### Scene 2: Encounter")
                    .contains("> **Author: Claude Sonnet 4**")
                    .contains("The characters meet...");

            System.out.println("✅ Libretto saved and verified: " + librettoPath.getFileName());
        } finally {
            restoreResourcePath();
        }
    }

    @Test
    void testSceneFilesCreation() throws IOException {
        // Given
        Opera.Scene scene1 = new Opera.Scene(1, "Test Scene", "GPT-4.1", "Scene content here");
        Opera testOpera = new Opera("Test Opera", "Test premise", List.of(scene1));

        // When - Save scene files
        String originalResourcePath = getResourcePath();
        setResourcePath(tempResourceDir.toString());
        
        try {
            LibrettoWriter.saveScenesToFiles(testOpera);

            // Then - Verify scene file
            Path sceneFile = tempResourceDir.resolve("scene_1_test_scene.txt");
            assertThat(sceneFile).exists().isRegularFile();

            String content = Files.readString(sceneFile);
            assertThat(content)
                    .contains("Scene 1: Test Scene")
                    .contains("Author: GPT-4.1")
                    .contains("Scene content here");

            System.out.println("✅ Scene file created and verified: " + sceneFile.getFileName());
        } finally {
            restoreResourcePath();
        }
    }

    @Test
    void testImageGenerationWorkflow() {
        // Given
        Opera.Scene scene = new Opera.Scene(1, "Visual Test", "GPT-4.1", 
                "A simple scene with a red apple on a table in bright lighting.");
        Opera testOpera = new Opera("Visual Test Opera", "Test premise", List.of(scene));

        // When - Generate images
        String originalResourcePath = getResourcePath();
        setResourcePath(tempResourceDir.toString());
        
        try {
            OperaImageGenerator.generateImages(testOpera);

            // Then - Verify image file was created
            Path imageFile = tempResourceDir.resolve("scene_1_illustration.png");
            assertThat(imageFile).exists().isRegularFile();
            
            // Verify it's a valid image file (has reasonable size)
            long fileSize = Files.size(imageFile);
            assertThat(fileSize).isGreaterThan(1000); // At least 1KB for a real image

            System.out.printf("✅ Image generated: %s (size: %d bytes)%n", 
                             imageFile.getFileName(), fileSize);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate or verify image", e);
        } finally {
            restoreResourcePath();
        }
    }

    @Test 
    void testOperaRecordMethods() {
        // Given
        Opera.Scene scene = new Opera.Scene(1, "Test Scene Name", "GPT-4.1", "Content");
        
        // When/Then - Test scene filename generation
        assertThat(scene.getFileName()).isEqualTo("scene_1_test_scene_name.txt");
        assertThat(scene.getImageFileName()).isEqualTo("scene_1_illustration.png");
        
        // Test opera structure
        Opera opera = new Opera("My Opera", "My premise", List.of(scene));
        assertThat(opera.title()).isEqualTo("My Opera");
        assertThat(opera.premise()).isEqualTo("My premise");
        assertThat(opera.scenes()).containsExactly(scene);
        
        System.out.println("✅ Opera record methods working correctly");
    }

    // Utility methods for temporarily changing resource path
    private String getResourcePath() {
        return OperaImageGenerator.RESOURCE_PATH;
    }

    private void setResourcePath(String path) {
        String originalLibretto = LibrettoWriter.RESOURCE_PATH;
        String originalImage = OperaImageGenerator.RESOURCE_PATH;
        
        LibrettoWriter.RESOURCE_PATH = path;
        OperaImageGenerator.RESOURCE_PATH = path;
        
        // Store original values for restoration
        this.originalLibrettoPath = originalLibretto;
        this.originalImagePath = originalImage;
    }
    
    private String originalLibrettoPath;
    private String originalImagePath;
    
    private void restoreResourcePath() {
        if (originalLibrettoPath != null) {
            LibrettoWriter.RESOURCE_PATH = originalLibrettoPath;
        }
        if (originalImagePath != null) {
            OperaImageGenerator.RESOURCE_PATH = originalImagePath;
        }
    }
}