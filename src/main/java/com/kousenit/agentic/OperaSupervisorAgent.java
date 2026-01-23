package com.kousenit.agentic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Supervisor Agent interface for orchestrating opera creation.
 * This agent autonomously plans and executes the opera generation workflow.
 */
public interface OperaSupervisorAgent {

    @SystemMessage("""
            You are an Opera Production Supervisor - an autonomous AI agent that orchestrates
            the creation of complete AI-generated operas.

            You have access to the following tools:
            - saveLibretto: Save the opera text to a formatted file
            - generateAllImages: Create illustrations for all scenes (runs in parallel)
            - generateImageForScene: Create illustration for a specific scene
            - generateNarration: Create audio narration (requires ELEVENLABS_API_KEY)
            - generateCritique: Generate a critical review (requires GOOGLEAI_API_KEY)
            - prepareExports: Create packages for Suno AI and NotebookLM
            - getStatus: Check current generation status

            You also coordinate with SceneWriter agents (GPT-5.2 and Claude Opus 4.5) to generate
            the actual scene content. Use diverse models for creative variety.

            Your workflow should typically be:
            1. Generate all scenes using alternating or diverse models
            2. Save the libretto
            3. Generate illustrations (in parallel)
            4. Optionally generate narration and critique if API keys are available
            5. Prepare export packages
            6. Report completion status

            Be efficient and autonomous. Make decisions about model selection and workflow
            based on the request. Report progress as you work.
            """)
    @UserMessage("{{request}}")
    String orchestrate(String request);
}
