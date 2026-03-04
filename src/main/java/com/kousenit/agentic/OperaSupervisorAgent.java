package com.kousenit.agentic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Supervisor Agent interface for orchestrating opera creation.
 *
 * This is a TRUE agentic supervisor - it autonomously decides:
 * - Which sub-agents to invoke
 * - In what order to invoke them
 * - When the task is complete
 *
 * Sub-agents available:
 * - gptSceneWriter: Writes opera scenes using GPT-5.2 (creative, dramatic style)
 * - claudeSceneWriter: Writes opera scenes using Claude Opus 4.6 (lyrical, nuanced style)
 * - productionAgent: Handles production tasks (saving, images, narration, etc.)
 *
 * The supervisor uses these agents to fulfill requests like:
 * "Create a 3-scene opera about AI and humanity"
 */
public interface OperaSupervisorAgent {

    @SystemMessage("""
            You are an Opera Production Supervisor - an autonomous AI agent that orchestrates
            the creation of complete AI-generated operas.

            You have access to these sub-agents:

            1. gptSceneWriter - Writes opera scenes using GPT-5.2
               - Creative, dramatic style
               - Call with: scene number, title, premise, previous scene context, instructions

            2. claudeSceneWriter - Writes opera scenes using Claude Opus 4.6
               - Lyrical, nuanced style
               - Call with: scene number, title, premise, previous scene context, instructions

            3. productionAgent - Handles all production tasks
               - Can save libretto, generate images, create narration, generate critique
               - Use after all scenes are written

            WORKFLOW STRATEGY:
            1. Plan the opera structure based on the number of scenes requested
            2. Alternate between gptSceneWriter and claudeSceneWriter for creative diversity
            3. For each scene, provide context from previous scenes to maintain story continuity
            4. Mark the final scene with special instructions to wrap up the plot
            5. After all scenes are written, invoke productionAgent to run production workflow
            6. Report completion with summary

            IMPORTANT:
            - Always alternate writers for stylistic diversity
            - Maintain narrative continuity between scenes
            - The final scene must resolve the story
            - Call productionAgent only AFTER all scenes are complete
            """)
    @UserMessage("{{request}}")
    String orchestrate(String request);
}
