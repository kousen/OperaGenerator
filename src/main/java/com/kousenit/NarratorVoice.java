package com.kousenit;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates narrator audio for opera stage directions using ElevenLabs.
 * This adds dramatic spoken narration without attempting to sing.
 */
public class NarratorVoice {
    
    private static final String ELEVEN_LABS_API_KEY = System.getenv("ELEVENLABS_API_KEY");
    private static final String API_URL = "https://api.elevenlabs.io/v1/text-to-speech/";
    
    // ElevenLabs voice IDs (you can customize these)
    private static final String NARRATOR_VOICE = "EXAVITQu4vr4xnSDxMaL"; // "Bella" - warm narrator
    private static final String CRITIC_VOICE = "ErXwobaYiN019PkySvjV"; // "Antoni" - authoritative
    
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    
    /**
     * Generate narrator audio for scene stage directions.
     */
    public Path generateSceneNarration(Opera.Scene scene, Path outputDir) throws IOException {
        // Extract stage directions (text in square brackets)
        String stageDirections = extractStageDirections(scene.content());
        
        if (stageDirections.isEmpty()) {
            return null;
        }
        
        // Add dramatic introduction
        String narration = String.format(
            "Scene %d: %s. %s",
            scene.number(),
            scene.title(),
            stageDirections
        );
        
        return generateAudio(narration, NARRATOR_VOICE, 
            outputDir.resolve("scene_" + scene.number() + "_narration.mp3"));
    }
    
    /**
     * Generate audio for the critic's review.
     */
    public Path generateCriticAudio(String review, String operaTitle, Path outputDir) throws IOException {
        // Take first paragraph or two of the review for audio
        String[] paragraphs = review.split("\n\n");
        String audioText = paragraphs[0];
        if (paragraphs.length > 1 && paragraphs[1].length() < 200) {
            audioText += " " + paragraphs[1];
        }
        
        return generateAudio(audioText, CRITIC_VOICE,
            outputDir.resolve("critic_review_audio.mp3"));
    }
    
    /**
     * Generate a dramatic opera introduction.
     */
    public Path generateOperaIntroduction(Opera opera, Path outputDir) throws IOException {
        String introduction = String.format(
            """
            Ladies and gentlemen, welcome to %s.
            An epic tale set in the wild jungles of Connecticut,
            where love blooms amidst ancient ruins and mechanical madness.
            Our story begins...
            """,
            opera.title()
        );
        
        return generateAudio(introduction, NARRATOR_VOICE,
            outputDir.resolve("opera_introduction.mp3"));
    }
    
    private Path generateAudio(String text, String voiceId, Path outputPath) throws IOException {
        var requestBody = HttpRequest.BodyPublishers.ofString(
            gson.toJson(Map.of(
                "text", text,
                "voice_settings", Map.of(
                    "stability", 0.75,
                    "similarity_boost", 0.75,
                    "style", 0.5,  // Add some dramatic flair
                    "use_speaker_boost", true
                )
            ))
        );
        
        var request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + voiceId))
            .header("xi-api-key", ELEVEN_LABS_API_KEY)
            .header("Accept", "audio/mpeg")
            .header("Content-Type", "application/json")
            .POST(requestBody)
            .build();
        
        try {
            // Ensure parent directory exists
            Files.createDirectories(outputPath.getParent());
            
            // Stream response directly to file
            var response = client.send(request, HttpResponse.BodyHandlers.ofFile(outputPath));
            
            if (response.statusCode() != 200) {
                throw new IOException("Failed to generate audio: " + response.statusCode());
            }
            
            System.out.println("Audio generated: " + outputPath);
            return outputPath;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }
    
    private String extractStageDirections(String content) {
        StringBuilder directions = new StringBuilder();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.trim().startsWith("[") && line.trim().endsWith("]")) {
                if (!directions.isEmpty()) directions.append(" ");
                directions.append(line.trim(), 1, line.trim().length() - 1);
            }
        }
        
        return directions.toString();
    }
}