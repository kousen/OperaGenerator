package com.kousenit;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility to organize opera files and create a complete libretto with embedded images.
 */
public class OperaOrganizer {
    
    private static final String RESOURCE_PATH = "src/main/resources";
    
    public static void organizeOpera(String currentTitle, String newTitle) throws IOException {
        // Create directory for the opera
        String folderName = newTitle.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
        Path operaDir = Paths.get(RESOURCE_PATH, folderName);
        Files.createDirectories(operaDir);
        
        System.out.println("📁 Created directory: " + operaDir);
        
        // Move all related files
        moveOperaFiles(currentTitle, operaDir);
        
        // Create complete libretto with images
        createCompleteLibretto(operaDir, newTitle);
        
        System.out.println("✅ Opera organized successfully!");
    }
    
    private static void moveOperaFiles(String currentTitle, Path operaDir) throws IOException {
        Path resourceDir = Paths.get(RESOURCE_PATH);
        
        // Files to move: libretto, scenes, and images
        try (Stream<Path> files = Files.list(resourceDir)) {
            files.filter(path -> {
                String filename = path.getFileName().toString();
                return filename.startsWith("scene_") || 
                       filename.equals(currentTitle.toLowerCase() + "_libretto.md");
            }).forEach(path -> {
                try {
                    Path destination = operaDir.resolve(path.getFileName());
                    Files.move(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("📄 Moved: " + path.getFileName());
                } catch (IOException e) {
                    System.err.println("Failed to move: " + path.getFileName());
                }
            });
        }
    }
    
    private static void createCompleteLibretto(Path operaDir, String newTitle) throws IOException {
        // Read the original libretto
        Path originalLibretto = operaDir.resolve("title_libretto.md");
        String librettoContent = Files.readString(originalLibretto);
        
        // Replace the title
        librettoContent = librettoContent.replaceFirst("# Title", "# " + newTitle);
        
        // For each scene, insert the corresponding image after the scene title
        for (int i = 1; i <= 5; i++) {
            String imageTag = String.format("\n\n![Scene %d Illustration](scene_%d_illustration.png)\n\n", i, i);
            String scenePattern = String.format("(### Scene %d: [^\\n]+)", i);
            librettoContent = librettoContent.replaceFirst(scenePattern, "$1" + imageTag);
        }
        
        // Save the complete libretto
        String completeFileName = newTitle.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_") + "_complete_libretto.md";
        Path completeLibretto = operaDir.resolve(completeFileName);
        Files.writeString(completeLibretto, librettoContent);
        
        System.out.println("📖 Created complete libretto with images: " + completeFileName);
        
        // Delete the original libretto
        Files.delete(originalLibretto);
    }
    
    public static void main(String[] args) {
        try {
            // Organize the current opera with a better title
            organizeOpera("title", "Hartford Ascending: An Opera of Love and Ruins");
        } catch (IOException e) {
            System.err.println("Error organizing opera: " + e.getMessage());
            e.printStackTrace();
        }
    }
}