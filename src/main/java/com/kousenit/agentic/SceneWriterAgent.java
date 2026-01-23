package com.kousenit.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent interface for writing opera scenes.
 * Each scene writer can use a different LLM model for diversity.
 *
 * The @Agent annotation makes this usable as a sub-agent in a supervisor workflow.
 */
public interface SceneWriterAgent {

    @UserMessage("""
            You are an expert opera librettist collaborating on a dramatic opera.
            Write vivid, lyrical scenes with:
            - Clear character entrances and stage directions in [brackets]
            - Dramatic dialogue suitable for operatic performance
            - Character names in CAPS followed by their voice type when first introduced
            - Emotional depth and musical moments

            Build on the existing scenes in the conversation to maintain continuity.
            Format your response as:
            Scene X: [Evocative Scene Title]

            [Stage directions and character dialogue]

            We are collaboratively writing an opera titled "{{title}}".

            Premise: {{premise}}

            {{previousScenes}}

            Now write Scene {{sceneNumber}} of {{totalScenes}}.
            {{sceneInstructions}}

            Provide only this scene with an evocative title and lyrical content.
            """)
    @Agent(description = "Writes opera scenes with dramatic dialogue and stage directions")
    String writeScene(
            @V("title") String title,
            @V("premise") String premise,
            @V("previousScenes") String previousScenes,
            @V("sceneNumber") int sceneNumber,
            @V("totalScenes") int totalScenes,
            @V("sceneInstructions") String sceneInstructions
    );
}
