package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "ELEVENLABS_API_KEY", matches = ".*")
class NarratorVoiceTest {
    private final NarratorVoice narrator = new NarratorVoice();

    @Test
    void generateIntroductionForHartfordAscending() throws Exception {
        // Use the existing Hartford opera
        Path operaDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        // Create simple opera object for introduction
        Opera opera = new Opera(
            "Hartford Ascending: An Opera of Love and Ruins",
            "A tale of love in post-climate change Connecticut",
            null
        );
        
        // Generate introduction audio
        Path audioPath = narrator.generateOperaIntroduction(opera, operaDir);
        
        assertThat(audioPath).exists();
        assertThat(Files.size(audioPath)).isGreaterThan(1000); // Should be at least 1KB
    }
    
    @Test
    void generateSceneNarration() throws Exception {
        Path outputDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        // Create a test scene with stage directions
        Opera.Scene scene = new Opera.Scene(
            1,
            "Encounter Beneath the Banyan Trees",
            """
            [The stage is lush and green, tangled with vines and enormous leaves.
            Birds call from above, and shafts of golden sunlight pierce the green gloom.]
            
            SANDRA: Who are you, spirit or man?
            
            [Thunder rumbles in the distance as they clasp hands]
            """,
            "Test"
        );
        
        Path audioPath = narrator.generateSceneNarration(scene, outputDir);
        
        assertThat(audioPath).exists();
    }
    
    @Test
    void generateCriticReview() throws Exception {
        NarratorVoice narrator = new NarratorVoice();
        Path outputDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        String review = """
            Hartford Ascending is a triumph of absurdist opera,
            blending environmental catastrophe with mechanical madness
            in a way that only AI could conceive.
            
            The robot's multilingual arias are particularly memorable,
            though one wonders if Verdi is spinning in his grave.
            """;
        
        Path audioPath = narrator.generateCriticAudio(review, "Hartford Ascending", outputDir);
        
        assertThat(audioPath).exists();
    }
}