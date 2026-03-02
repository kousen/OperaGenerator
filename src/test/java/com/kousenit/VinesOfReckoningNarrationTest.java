package com.kousenit;

import com.kousenit.tags.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generates dramatic audio narration for "The Vines of Reckoning: A Connecticut Jungle Opera".
 * Produces an introduction and per-scene narrations using ElevenLabs text-to-speech.
 */
@EnabledIfEnvironmentVariable(named = "ELEVENLABS_API_KEY", matches = ".+")
@IntegrationTest
class VinesOfReckoningNarrationTest {

    private static final Path OPERA_DIR = Paths.get(
            "src/main/resources/the_vines_of_reckoning_a_connecticut_jungle_opera");

    @Test
    void generateAllNarration() throws Exception {
        // Load the opera from JSON
        Path jsonPath = OPERA_DIR.resolve("opera.json");
        assertThat(jsonPath).exists();
        Opera opera = OperaSerializer.load(jsonPath);

        System.out.println("Generating narration for: " + opera.title());
        System.out.println("Number of scenes: " + opera.scenes().size());

        NarratorVoice narrator = new NarratorVoice();

        // Step 1: Generate dramatic introduction
        System.out.println("\n--- Generating opera introduction ---");
        Path introAudio = narrator.generateOperaIntroduction(opera, OPERA_DIR);
        assertThat(introAudio).exists();
        assertThat(Files.size(introAudio)).isGreaterThan(1000);
        System.out.println("Introduction audio: " + introAudio.getFileName()
                + " (" + Files.size(introAudio) + " bytes)");

        // Step 2: Generate per-scene narrations
        for (Opera.Scene scene : opera.scenes()) {
            System.out.printf("%n--- Generating narration for Scene %d: %s ---%n",
                    scene.number(), scene.title());
            Path sceneAudio = narrator.generateSceneNarration(scene, OPERA_DIR);
            if (sceneAudio != null) {
                assertThat(sceneAudio).exists();
                assertThat(Files.size(sceneAudio)).isGreaterThan(1000);
                System.out.println("Scene " + scene.number() + " audio: "
                        + sceneAudio.getFileName()
                        + " (" + Files.size(sceneAudio) + " bytes)");
            } else {
                System.out.println("Scene " + scene.number()
                        + ": No stage directions extracted for narration");
            }
        }

        // Summary
        System.out.println("\n=== Narration Generation Complete ===");
        System.out.println("Opera: " + opera.title());
        try (var files = Files.list(OPERA_DIR)) {
            files.filter(p -> p.toString().endsWith(".mp3"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            System.out.printf("  %s (%,d bytes)%n",
                                    p.getFileName(), Files.size(p));
                        } catch (Exception e) {
                            System.out.println("  " + p.getFileName());
                        }
                    });
        }
    }
}
