package com.kousenit;

import java.io.IOException;
import java.nio.file.Path;

public class IntegratedOperaGenerator {

    public static void main(String[] args) {
        try {
            // Configuration
            String operaTitle = args.length > 0 ? args[0] : null; // Let AI suggest if not provided
            int numberOfScenes = args.length > 1 ? Integer.parseInt(args[1]) : 5;

            System.out.println("🎭 Starting Opera Generation Process...\n");

            // Step 1: Generate the opera with alternating AI models
            System.out.println("📝 Step 1: Generating opera scenes with GPT-5.2 and Claude Opus 4.5...");
            Conversation conversation = new Conversation();
            Opera opera = conversation.generateOpera(operaTitle, numberOfScenes);

            System.out.printf("\n✅ Generated opera: \"%s\" with %d scenes\n\n",
                    opera.title(), opera.scenes().size());

            // Step 2: Save complete opera with organized directory and formatting
            System.out.println("💾 Step 2: Saving complete opera with automatic stanza formatting...");
            var librettoPath = LibrettoWriter.saveCompleteOpera(opera);
            System.out.println("✅ Opera saved with beautiful formatting\n");

            // Step 3: Generate audio narration (optional - requires ElevenLabs API key)
            if (System.getenv("ELEVENLABS_API_KEY") != null) {
                System.out.println("🎙️ Step 3: Generating audio narration...");
                try {
                    NarratorVoice narrator = new NarratorVoice();
                    Path operaDir = librettoPath.getParent();

                    // Generate dramatic introduction
                    Path introAudio = narrator.generateOperaIntroduction(opera, operaDir);
                    System.out.println("   ✅ Introduction narration created: " + introAudio.getFileName());

                    // Generate narration for each scene's stage directions
                    int narratedScenes = 0;
                    for (Opera.Scene scene : opera.scenes()) {
                        Path sceneAudio = narrator.generateSceneNarration(scene, operaDir);
                        if (sceneAudio != null) {
                            System.out.printf("   ✅ Scene %d narration created: %s%n",
                                            scene.number(), sceneAudio.getFileName());
                            narratedScenes++;
                        }
                    }

                    if (narratedScenes == 0) {
                        System.out.println("   ℹ️ No stage directions found to narrate in scenes");
                    }
                    System.out.println("✅ Audio narration complete\n");
                } catch (Exception e) {
                    System.out.println("   ⚠️ Could not generate audio narration: " + e.getMessage() + "\n");
                }
            } else {
                System.out.println("ℹ️ Step 3: Skipping audio narration (set ELEVENLABS_API_KEY to enable)\n");
            }

            // Step 4: Generate illustrations for each scene using Gemini Nano Banana
            System.out.println("🎨 Step 4: Generating illustrations for each scene with Nano Banana...");
            System.out.println("⚠️  This may take several minutes due to API rate limiting...\n");
            GeminiImageGenerator.generateImages(opera);
            System.out.println("✅ Illustrations generated\n");

            // Final summary
            System.out.println("🎉 Opera Generation Complete!");
            System.out.println("=".repeat(50));
            System.out.printf("📖 Opera Title: %s%n", opera.title());
            System.out.printf("🎬 Total Scenes: %d%n", opera.scenes().size());
            System.out.printf("📁 Libretto File: %s%n", librettoPath.getFileName());
            System.out.println("🖼️  Individual scene files and illustrations created in production_runs/");

            // List the generated files
            System.out.println("\n📋 Generated Files:");
            opera.scenes().forEach(scene -> {
                System.out.printf("   • %s%n", scene.getFileName());
                System.out.printf("   • %s%n", scene.getImageFileName());
            });

            // List audio files if generated
            if (System.getenv("ELEVENLABS_API_KEY") != null) {
                System.out.println("\n🎵 Audio Files:");
                System.out.println("   • opera_introduction.mp3");
                opera.scenes().forEach(scene ->
                    System.out.printf("   • scene_%d_narration.mp3 (if stage directions exist)%n", scene.number())
                );
            }

            // Step 6: Prepare export packages for external tools
            System.out.println("📦 Step 6: Preparing exports for external tools...");
            try {
                Path operaDir = librettoPath.getParent();

                // Generate Suno AI music prompts
                ExternalToolsPreparer.generateSunoPrompts(opera, operaDir);

                // Prepare NotebookLM package
                ExternalToolsPreparer.prepareNotebookLMPackage(opera, operaDir);

                System.out.println("✅ External tool packages prepared\n");
            } catch (Exception e) {
                System.out.println("⚠️ Could not prepare external packages: " + e.getMessage() + "\n");
            }

            // Optional: Generate a critical review (requires a Google AI API key)
            if (System.getenv("GOOGLEAI_API_KEY") != null) {
                System.out.println("📰 Step 7: Generating critical review...");
                try {
                    OperaCritic critic = new OperaCritic();
                    Path operaDir = librettoPath.getParent();
                    critic.reviewAndSave(operaDir, opera.title());
                    System.out.println("✅ Critical review generated");

                    // Generate audio for the critic's review if ElevenLabs is available
                    if (System.getenv("ELEVENLABS_API_KEY") != null) {
                        try {
                            Path critiquePath = operaDir.resolve(opera.title().toLowerCase().replace(" ", "_") + "_critique.md");
                            String review = java.nio.file.Files.readString(critiquePath);
                            NarratorVoice narrator = new NarratorVoice();
                            Path criticAudio = narrator.generateCriticAudio(review, opera.title(), operaDir);
                            System.out.println("   🎙️ Critic audio review created: " + criticAudio.getFileName());
                        } catch (Exception e) {
                            System.out.println("   ⚠️ Could not generate critic audio: " + e.getMessage());
                        }
                    }
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("⚠️  Could not generate critique: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error during opera generation: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid number of scenes specified. Please provide a valid integer.");
            System.out.println("Usage: java IntegratedOperaGenerator [opera-title] [number-of-scenes]");
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
        }
    }
}
