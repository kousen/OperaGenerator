package com.kousenit.embabel;

import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.LlmOptions;
import com.kousenit.Opera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Embabel agent for generating opera scenes using alternating AI models.
 * <p>
 * This agent uses GOAP planning to coordinate scene generation:
 * - Odd scenes (1, 3, 5...) are written by GPT-5.2
 * - Even scenes (2, 4, 6...) are written by Claude Opus 4.5
 * <p>
 * Domain objects flow through the blackboard pattern:
 * SceneRequest → Scene → scenes accumulate → Opera
 */
@Agent(description = "Generates opera scenes using alternating AI models for creative diversity")
@Component
public class OperaSceneStages {

    private static final Logger logger = LoggerFactory.getLogger(OperaSceneStages.class);

    // Model names matching Embabel's available models
    private static final String GPT_MODEL = "gpt-5";
    private static final String CLAUDE_MODEL = "claude-opus-4-1";

    // Injected AI for manual mode (when OperationContext is null)
    private final Ai ai;

    public OperaSceneStages(Ai ai) {
        this.ai = ai;
    }

    private static final String SCENE_WRITER_PROMPT = """
            You are an expert opera librettist collaborating on a dramatic opera.
            Write vivid, lyrical scenes with:
            - Clear character entrances and stage directions in [brackets]
            - Dramatic dialogue suitable for operatic performance
            - Character names in CAPS followed by their voice type when first introduced
            - Emotional depth and musical moments

            Build on the existing scenes to maintain continuity.
            Format your response as:
            Scene X: [Evocative Scene Title]

            [Stage directions and character dialogue]

            Opera Title: %s

            Premise: %s

            Previous scenes:
            %s

            Now write Scene %d of %d.
            %s

            Provide only this scene with an evocative title and lyrical content.
            """;

    /**
     * Domain record representing a request to generate scenes for an opera.
     */
    public record SceneGenerationRequest(
            String title,
            String premise,
            int totalScenes
    ) {}

    /**
     * Domain record representing the accumulated state during scene generation.
     */
    public record SceneGenerationState(
            String title,
            String premise,
            int totalScenes,
            List<Opera.Scene> completedScenes
    ) {
        public SceneGenerationState {
            completedScenes = new ArrayList<>(completedScenes);
        }

        public int nextSceneNumber() {
            return completedScenes.size() + 1;
        }

        public boolean isComplete() {
            return completedScenes.size() >= totalScenes;
        }

        public String previousScenesContext() {
            if (completedScenes.isEmpty()) {
                return "(No previous scenes)";
            }
            StringBuilder sb = new StringBuilder();
            for (Opera.Scene scene : completedScenes) {
                sb.append("--- Scene %d: %s (by %s) ---\n".formatted(
                        scene.number(), scene.title(), scene.author()));
                sb.append(scene.content()).append("\n\n");
            }
            return sb.toString();
        }

        public SceneGenerationState withScene(Opera.Scene scene) {
            List<Opera.Scene> newScenes = new ArrayList<>(completedScenes);
            newScenes.add(scene);
            return new SceneGenerationState(title, premise, totalScenes, newScenes);
        }
    }

    /**
     * Initialize the scene generation process from a request.
     */
    @Action(description = "Initialize scene generation state from a request")
    public SceneGenerationState initializeGeneration(SceneGenerationRequest request) {
        logger.info("Initializing scene generation for '{}' with {} scenes",
                request.title(), request.totalScenes());
        return new SceneGenerationState(
                request.title(),
                request.premise(),
                request.totalScenes(),
                List.of()
        );
    }

    /**
     * Write the next scene using the appropriate model based on scene number.
     * Odd scenes use GPT-5.2, even scenes use Claude Opus 4.5.
     */
    @Action(description = "Write the next opera scene using alternating AI models")
    public SceneGenerationState writeNextScene(
            SceneGenerationState state,
            OperationContext context
    ) {
        if (state.isComplete()) {
            logger.info("All scenes complete, no more to write");
            return state;
        }

        int sceneNumber = state.nextSceneNumber();
        boolean isFinalScene = sceneNumber == state.totalScenes();
        boolean useGpt = (sceneNumber % 2 == 1); // Odd scenes use GPT

        String modelName = useGpt ? GPT_MODEL : CLAUDE_MODEL;
        String modelDisplayName = useGpt ? "GPT-5.2" : "Claude Opus 4.5";

        String sceneInstructions = isFinalScene
                ? "This is the FINAL scene. Tie off major plot threads, resolve the romantic arc, and deliver a decisive ending."
                : "Set the stage for upcoming scenes so the opera can naturally progress to its finale.";

        String prompt = SCENE_WRITER_PROMPT.formatted(
                state.title(),
                state.premise(),
                state.previousScenesContext(),
                sceneNumber,
                state.totalScenes(),
                sceneInstructions
        );

        logger.info("Writing Scene {} with {}...", sceneNumber, modelDisplayName);

        // Use injected Ai for manual mode, or context.ai() for agentic mode
        // GPT-5 only supports temperature = 1.0; Claude supports variable temperature
        Ai aiToUse = (context != null) ? context.ai() : ai;
        double temperature = useGpt ? 1.0 : 0.8;
        String sceneContent = aiToUse
                .withLlm(LlmOptions.withModel(modelName).withTemperature(temperature))
                .generateText(prompt);

        Opera.Scene scene = Opera.Scene.parse(sceneNumber, sceneContent, modelDisplayName);
        logger.info("Completed Scene {}: '{}' by {}", scene.number(), scene.title(), scene.author());

        return state.withScene(scene);
    }

    /**
     * Generate an opera title if one isn't provided.
     */
    @Action(description = "Generate a creative title for the opera")
    public String generateTitle(String premise, OperationContext context) {
        String prompt = """
                Based on this opera premise, suggest a creative and evocative title.
                The title should capture the essence of the story.
                Respond with ONLY the title, nothing else.

                Premise:
                %s
                """.formatted(premise);

        logger.info("Generating opera title...");

        // Use injected Ai for manual mode, or context.ai() for agentic mode
        // GPT-5 only supports temperature = 1.0
        Ai aiToUse = (context != null) ? context.ai() : ai;
        String response = aiToUse
                .withLlm(LlmOptions.withModel(GPT_MODEL).withTemperature(1.0))
                .generateText(prompt);

        String title = response.trim()
                .replaceAll("^[\"']|[\"']$", "")
                .replaceAll("\\n.*", "")
                .trim();

        logger.info("Generated title: {}", title);
        return title.isEmpty() ? "The Opera of Hartford" : title;
    }

    /**
     * Build the final Opera object from accumulated scenes.
     * This is the goal state for scene generation.
     */
    @AchievesGoal(description = "Complete scene generation by building the final Opera")
    @Action(description = "Build the final Opera object from completed scenes")
    public Opera buildOpera(SceneGenerationState state) {
        if (state.completedScenes().isEmpty()) {
            throw new IllegalStateException("Cannot build opera with no scenes");
        }
        logger.info("Building opera '{}' with {} scenes",
                state.title(), state.completedScenes().size());
        return new Opera(state.title(), state.premise(), state.completedScenes());
    }
}
