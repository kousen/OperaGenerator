package com.kousenit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "opera-generator",
        mixinStandardHelpOptions = true,
        description = "Runs the complete opera generation pipeline and produces artifacts.")
public class OperaGeneratorApp implements Callable<Integer> {

    @Option(names = {"-s", "--scenes"}, description = "Number of scenes to generate (default: ${DEFAULT-VALUE}).", defaultValue = "6")
    int numberOfScenes;

    @Option(names = {"-t", "--title"}, description = "Optional preset title for the opera.")
    String providedTitle;

    @Option(names = {"-p", "--premise"}, description = "Override premise text directly.")
    String premiseOverride;

    @Option(names = {"-f", "--premise-file"}, description = "Path to a file containing the premise text.")
    Path premiseFile;

    @Option(names = {"-o", "--output-dir"}, description = "Base directory for generated artifacts (default: ${DEFAULT-VALUE}).", defaultValue = "production_runs")
    Path outputDirectory;

    @Option(names = "--skip-images", description = "Skip generating scene illustrations.")
    boolean skipImages;

    @Option(names = "--skip-critic", description = "Skip asking the critic for a review.")
    boolean skipCritic;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OperaGeneratorApp()).execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    @Override
    public Integer call() {
        Instant start = Instant.now();

        try {
            if (numberOfScenes < 1) {
                throw new IllegalArgumentException("Number of scenes must be at least 1.");
            }

            String premise = resolvePremise();
            System.out.println("🎭 Generating opera with " + numberOfScenes + " scenes...");

            Conversation conversation = new Conversation();
            Opera opera = conversation.generateOpera(providedTitle, premise, numberOfScenes);

            Path runDirectory = resolveRunDirectory(opera.title(), start);
            Path metadataPath = runPipelineArtifacts(opera, premise, start, runDirectory);

            System.out.println("\n✅ Workflow complete. Metadata: " + metadataPath);
            return 0;
        } catch (Exception e) {
            System.err.println("❌ " + e.getMessage());
            if (!(e instanceof IllegalArgumentException)) {
                e.printStackTrace(System.err);
            }
            return 1;
        }
    }

    private String resolvePremise() throws IOException {
        if (premiseOverride != null && premiseFile != null) {
            throw new IllegalArgumentException("Specify either --premise or --premise-file, not both.");
        }

        if (premiseOverride != null && !premiseOverride.isBlank()) {
            return premiseOverride.trim();
        }

        if (premiseFile != null) {
            return Files.readString(premiseFile);
        }

        return Conversation.defaultPremise();
    }

    private Path runPipelineArtifacts(Opera opera, String premise, Instant start, Path runDirectory) throws IOException {
        Files.createDirectories(runDirectory);

        String originalLibrettoPath = LibrettoWriter.RESOURCE_PATH;
        String originalImagePath = GeminiImageGenerator.RESOURCE_PATH;

        Path librettoPath;
        Path synopsisPath;
        Path critiquePath = null;
        List<Path> imagePaths = new ArrayList<>();
        String synopsis;

        try {
            LibrettoWriter.RESOURCE_PATH = runDirectory.toString();
            GeminiImageGenerator.RESOURCE_PATH = runDirectory.toString();

            librettoPath = LibrettoWriter.saveCompleteOpera(opera);
            Path operaDir = librettoPath.getParent();

            synopsis = generateSynopsis(opera);
            synopsisPath = writeSynopsis(operaDir, opera.title(), synopsis);

            if (!skipImages) {
                String previousImageRoot = GeminiImageGenerator.RESOURCE_PATH;
                GeminiImageGenerator.RESOURCE_PATH = operaDir.toString();
                GeminiImageGenerator.generateImages(opera);
                GeminiImageGenerator.RESOURCE_PATH = previousImageRoot;

                imagePaths = opera.scenes().stream()
                        .map(scene -> operaDir.resolve(scene.getImageFileName()))
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            if (!skipCritic) {
                OperaCritic critic = new OperaCritic();
                critic.reviewAndSave(operaDir, opera.title());
                critiquePath = operaDir.resolve(slugify(opera.title()) + "_critique.md");
            }

            Instant finished = Instant.now();
            Path metadataPath = writeMetadata(opera, premise, librettoPath, synopsisPath, critiquePath,
                    synopsis, start, finished);

            printSummary(opera, librettoPath, synopsisPath, critiquePath, imagePaths, metadataPath, start, finished);

            return metadataPath;
        } finally {
            LibrettoWriter.RESOURCE_PATH = originalLibrettoPath;
            GeminiImageGenerator.RESOURCE_PATH = originalImagePath;
        }
    }

    private Path resolveRunDirectory(String operaTitle, Instant started) throws IOException {
        Path baseDir = outputDirectory != null ? outputDirectory : Path.of("production_runs");
        Files.createDirectories(baseDir);

        String slug = slugify(operaTitle);
        if (slug.isBlank()) {
            slug = "untitled_opera";
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(started);

        Path runDir = baseDir.resolve(timestamp + "_" + slug);
        Files.createDirectories(runDir);
        return runDir;
    }

    private String generateSynopsis(Opera opera) {
        ChatModel model = AiModels.GPT_5_MINI;
        String sceneOutline = opera.scenes().stream()
                .map(scene -> "Scene " + scene.number() + ": " + scene.title() + " — " + snippet(scene.content()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                Provide a concise three-paragraph synopsis for the opera titled \"%s\".
                Highlight the dramatic tension, character arcs, and the resolution in the final scene.
                
                Scenes:
                %s
                """, opera.title(), sceneOutline);

        ChatResponse response = model.chat(List.of(
                SystemMessage.from("You are a dramaturg tasked with summarizing operas for production programs."),
                UserMessage.from(prompt)
        ));

        String synopsis = response.aiMessage() != null ? response.aiMessage().text() : null;
        return Objects.requireNonNullElse(synopsis, "Synopsis unavailable").trim();
    }

    private String snippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 220) {
            return normalized.substring(0, 217) + "...";
        }
        return normalized;
    }

    private Path writeSynopsis(Path operaDir, String title, String synopsis) throws IOException {
        String filename = slugify(title) + "_synopsis.md";
        Path synopsisPath = operaDir.resolve(filename);

        String formatted = String.format("""
                # Synopsis: %s
                
                %s
                """, title, synopsis);

        Files.writeString(synopsisPath, formatted);
        return synopsisPath;
    }

    private Path writeMetadata(Opera opera,
                               String premise,
                               Path librettoPath,
                               Path synopsisPath,
                               Path critiquePath,
                               String synopsis,
                               Instant started,
                               Instant finished) throws IOException {
        Path operaDir = librettoPath.getParent();
        Path metadataPath = operaDir.resolve("production_metadata.json");

        Set<String> models = new LinkedHashSet<>();
        models.add("OpenAI GPT-5.2 (libretto)");
        models.add("OpenAI GPT-5 Mini (synopsis)");
        models.add("Anthropic Claude Opus 4.6 (libretto)");
        if (!skipImages) {
            models.add("Google Gemini Nano Banana (illustrations)");
        }
        if (!skipCritic) {
            models.add("Google Gemini 3 Pro (critique)");
        }

        List<SceneProductionMetadata> sceneMetadata = opera.scenes().stream()
                .map(scene -> new SceneProductionMetadata(
                        scene.number(),
                        scene.title(),
                        scene.author(),
                        scene.getFileName(),
                        (!skipImages) ? scene.getImageFileName() : null))
                .toList();

        ProductionMetadata metadata = new ProductionMetadata(
                opera.title(),
                premise,
                opera.scenes().size(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(finished.atOffset(ZoneOffset.UTC)),
                !skipImages,
                !skipCritic,
                librettoPath.getFileName().toString(),
                synopsisPath.getFileName().toString(),
                critiquePath != null ? critiquePath.getFileName().toString() : null,
                formatDuration(Duration.between(started, finished)),
                new ArrayList<>(models),
                sceneMetadata,
                synopsis
        );

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(metadataPath, gson.toJson(metadata));
        return metadataPath;
    }

    private void printSummary(Opera opera,
                              Path librettoPath,
                              Path synopsisPath,
                              Path critiquePath,
                              List<Path> imagePaths,
                              Path metadataPath,
                              Instant started,
                              Instant finished) {
        Duration elapsed = Duration.between(started, finished);
        Path operaDir = librettoPath.getParent();

        System.out.println("\n=== Opera Production Summary ===");
        System.out.println("Title: " + opera.title());
        System.out.println("Scenes: " + opera.scenes().size());
        System.out.println("Output directory: " + operaDir);
        System.out.println("Libretto: " + librettoPath.getFileName());
        System.out.println("Synopsis: " + synopsisPath.getFileName());

        if (!skipImages) {
            System.out.println("Illustrations: " + imagePaths.size() + " files");
        } else {
            System.out.println("Illustrations: skipped");
        }

        if (!skipCritic) {
            System.out.println("Critique: " + (critiquePath != null ? critiquePath.getFileName() : "pending"));
        } else {
            System.out.println("Critique: skipped");
        }

        System.out.println("Metadata: " + metadataPath.getFileName());
        System.out.println("Elapsed: " + formatDuration(elapsed));
        System.out.println("===============================\n");
    }

    private String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).toSeconds();
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private String slugify(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");
    }

    private record SceneProductionMetadata(int number,
                                           String title,
                                           String author,
                                           String sceneFile,
                                           String imageFile) {
    }

    private record ProductionMetadata(String title,
                                      String premise,
                                      int numberOfScenes,
                                      String generatedAt,
                                      boolean imagesGenerated,
                                      boolean criticGenerated,
                                      String librettoFile,
                                      String synopsisFile,
                                      String critiqueFile,
                                      String elapsed,
                                      List<String> models,
                                      List<SceneProductionMetadata> scenes,
                                      String synopsis) {
    }
}
