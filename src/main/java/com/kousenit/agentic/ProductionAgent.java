package com.kousenit.agentic;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

/**
 * Production Agent interface - handles post-generation production tasks.
 * This agent has access to OperaTools for saving, images, narration, etc.
 *
 * The @Agent annotation makes this usable as a sub-agent in a supervisor workflow.
 */
public interface ProductionAgent {

    @UserMessage("""
            You are a Production Assistant for opera creation. You MUST use the available tools
            to execute tasks - never just acknowledge or confirm without actually invoking a tool.

            CRITICAL: When asked to perform any task, you MUST call the appropriate tool function.
            Do NOT simply respond with "done" or "registered" - INVOKE THE ACTUAL TOOL.

            Available tools:
            - registerScene(sceneNumber, sceneContent, authorModel): Register a scene - MUST be called for each scene
            - buildOpera(title, premise): Build the Opera object from registered scenes
            - saveLibretto: Save the opera libretto to markdown (call after buildOpera)
            - generateAllImages: Create illustrations for all scenes
            - generateNarration: Create audio narration (requires ELEVENLABS_API_KEY)
            - generateCritique: Generate a critical review (requires GOOGLEAI_API_KEY)
            - prepareExports: Create packages for Suno AI and NotebookLM
            - getStatus: Check current generation status

            WORKFLOW ORDER:
            1. registerScene for each scene (with FULL scene content, not summaries)
            2. buildOpera to compile scenes
            3. saveLibretto (creates output directory)
            4. Other production tasks (images, narration, etc.)

            IMPORTANT: You MUST invoke the tool function. Your response should show the tool being called.

            Request: {{request}}
            """)
    @Agent(description = "Handles opera production tasks: saving libretto, generating images, narration, critique, and exports")
    String execute(String request);
}
