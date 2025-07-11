package com.kousenit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LibrettoWriter {
    
    static String RESOURCE_PATH = "src/main/resources"; // Package-private for testing
    
    public static Path saveLibretto(Opera opera) throws IOException {
        String filename = opera.title().replaceAll("\\s+", "_").toLowerCase() + "_libretto.md";
        Path outputPath = Paths.get(RESOURCE_PATH, filename);
        
        StringBuilder markdown = new StringBuilder();
        
        // Add title and premise
        markdown.append("# ").append(opera.title()).append("\n\n");
        markdown.append("## Premise\n\n");
        markdown.append(opera.premise()).append("\n\n");
        markdown.append("---\n\n");
        
        // Add each scene
        for (Opera.Scene scene : opera.scenes()) {
            markdown.append("### Scene ").append(scene.number())
                    .append(": ").append(scene.title()).append("\n\n");
            
            // Add author attribution
            markdown.append("> **Author: ").append(scene.author()).append("**\n\n");
            
            // Add scene content
            markdown.append(scene.content()).append("\n\n");
            
            // Add separator between scenes
            markdown.append("---\n\n");
        }
        
        // Ensure directory exists
        Files.createDirectories(outputPath.getParent());
        
        // Write the file
        Files.writeString(outputPath, markdown.toString());
        System.out.println("Libretto saved to: " + outputPath);
        
        return outputPath;
    }
    
    public static void saveScenesToFiles(Opera opera) throws IOException {
        Path scenesDir = Paths.get(RESOURCE_PATH);
        Files.createDirectories(scenesDir);
        
        for (Opera.Scene scene : opera.scenes()) {
            Path scenePath = scenesDir.resolve(scene.getFileName());
            
            String content = String.format("""
                    Scene %d: %s
                    Author: %s
                    
                    %s
                    """, scene.number(), scene.title(), scene.author(), scene.content());
            
            Files.writeString(scenePath, content);
            System.out.println("Scene saved to: " + scenePath);
        }
    }
}