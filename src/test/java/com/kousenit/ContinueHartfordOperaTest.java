package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ContinueHartfordOperaTest {
    
    @Test
    void continueHartfordOpera() throws IOException {
        System.out.println("🎭 Continuing 'Hartford Ascending: An Opera of Love and Ruins'...\n");
        
        // Create the conversation system
        Conversation conversation = new Conversation();
        
        // Read the existing premise from the libretto
        Path librettoPath = Paths.get("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins/hartford_ascending_an_opera_of_love_and_ruins_complete_libretto.md");
        String librettoContent = Files.readString(librettoPath);
        
        // Extract the premise (between ## Premise and ---)
        int premiseStart = librettoContent.indexOf("## Premise") + "## Premise".length();
        int premiseEnd = librettoContent.indexOf("---", premiseStart);
        String premise = librettoContent.substring(premiseStart, premiseEnd).trim();
        
        // Create continuation context
        String continuationContext = premise + """
                
                
                IMPORTANT CONTINUATION CONTEXT:
                The opera has already progressed through 5 scenes where Sandra (soprano explorer) 
                and Lucian (tenor poet) met and fell in love while being pursued by Maximilian 
                (baritone government agent) and his giant robot through the Connecticut jungle.
                
                Scene 5 ended at the gates of Hartford with everyone in confrontation:
                - Sandra and Lucian stand defiantly at the ruined marble arch
                - Sandra's locket is glowing and responding to the ancient stones
                - Maximilian and his robot have arrived to stop them
                - The city itself is awakening with mysterious energy
                - Everyone is frozen in dramatic tableau
                
                Continue from Scene 6. You must complete the opera in 3 scenes by:
                1. Resolving the confrontation at Hartford's gates
                2. Revealing what Hartford's secret actually is
                3. Determining the fate of all characters
                4. Ending with a proper operatic finale
                
                Start with Scene 6 and alternate between the AI models as before.
                """;
        
        // Generate the continuation (3 more scenes: 6, 7, 8)
        Opera continuation = conversation.generateOpera(
                "Hartford Ascending: An Opera of Love and Ruins - Conclusion", 
                continuationContext, 
                3
        );
        
        // Adjust scene numbers to be 6, 7, 8
        List<Opera.Scene> continuationScenes = new ArrayList<>();
        for (int i = 0; i < continuation.scenes().size(); i++) {
            Opera.Scene original = continuation.scenes().get(i);
            continuationScenes.add(new Opera.Scene(
                    i + 6,  // Start from scene 6
                    original.title(),
                    original.author(),
                    original.content()
            ));
        }
        
        // Create directory for the continuation
        Path continuationDir = Paths.get("src/main/resources/hartford_ascending_continuation");
        Files.createDirectories(continuationDir);
        
        // Save the continuation scenes
        System.out.println("\n💾 Saving continuation scenes...");
        for (Opera.Scene scene : continuationScenes) {
            Path scenePath = continuationDir.resolve(scene.getFileName());
            String content = String.format("""
                    Scene %d: %s
                    Author: %s
                    
                    %s
                    """, scene.number(), scene.title(), scene.author(), scene.content());
            Files.writeString(scenePath, content);
            System.out.println("📄 Saved: " + scene.getFileName());
        }
        
        // Generate images for the new scenes
        System.out.println("\n🎨 Generating illustrations for continuation scenes...");
        System.out.println("⚠️  This will take about 3 minutes for 3 scenes...");
        
        Opera continuationOpera = new Opera(
                "Hartford Ascending Continuation",
                continuationContext,
                continuationScenes
        );
        
        // Set the resource path to the continuation directory
        String originalPath = OperaImageGenerator.RESOURCE_PATH;
        OperaImageGenerator.RESOURCE_PATH = continuationDir.toString();
        
        try {
            OperaImageGenerator.generateImages(continuationOpera);
        } finally {
            // Restore original path
            OperaImageGenerator.RESOURCE_PATH = originalPath;
        }
        
        System.out.println("\n✅ Continuation complete!");
        System.out.println("📁 New scenes and images saved in: " + continuationDir);
        System.out.println("\n📝 Next: Review the scenes and merge with the original opera!");
    }
}