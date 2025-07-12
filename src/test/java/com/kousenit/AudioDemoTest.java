package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demo test that generates and plays audio - perfect for live presentation!
 */
@EnabledIfEnvironmentVariable(named = "ELEVENLABS_API_KEY", matches = ".*")
class AudioDemoTest {
    
    @Test
    void generateAndPlayOperaIntroduction() throws Exception {
        NarratorVoice narrator = new NarratorVoice();
        Path outputDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        // Create Hartford opera for demo
        Opera opera = new Opera(
            "Hartford Ascending: An Opera of Love and Ruins",
            "An epic tale of love in post-climate change Connecticut",
            null
        );
        
        System.out.println("🎭 Generating opera introduction...");
        Path audioFile = narrator.generateOperaIntroduction(opera, outputDir);
        
        System.out.println("🎵 Now playing the introduction...");
        AudioPlayer.play(audioFile);
        
        System.out.println("🎉 Demo complete!");
    }
    
    @Test
    void generateAndPlaySceneNarration() throws Exception {
        NarratorVoice narrator = new NarratorVoice();
        Path outputDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        // Create a dramatic scene
        Opera.Scene scene = new Opera.Scene(
            1,
            "Encounter Beneath the Banyan Trees",
            """
            [The stage is lush and green, tangled with vines and enormous leaves.
            Birds call from above, and shafts of golden sunlight pierce the green gloom.
            The explorer SANDRA hacks through the undergrowth, sweat on her brow, map in hand.]
            
            SANDRA: Who are you, spirit or man?
            
            [Thunder rumbles in the distance as mysterious music begins to play]
            """,
            "GPT-4"
        );
        
        System.out.println("🎭 Generating scene narration...");
        Path audioFile = narrator.generateSceneNarration(scene, outputDir);
        
        System.out.println("🎵 Now playing scene narration...");
        AudioPlayer.play(audioFile);
        
        System.out.println("🎉 Scene demo complete!");
    }
    
    @Test
    void playExistingAudioFiles() throws Exception {
        Path operaDir = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        
        // Try to play any existing audio files
        try (var files = java.nio.file.Files.list(operaDir)) {
            files.filter(path -> path.toString().endsWith(".mp3"))
                 .forEach(audioFile -> {
                     try {
                         System.out.println("\n🎵 Playing: " + audioFile.getFileName());
                         AudioPlayer.play(audioFile);
                         Thread.sleep(1000); // Brief pause between files
                     } catch (Exception e) {
                         System.err.println("❌ Could not play: " + audioFile.getFileName());
                     }
                 });
        }
    }
}