package com.kousenit.agentic;

import com.kousenit.Opera;
import com.kousenit.tags.ExpensiveTest;
import com.kousenit.tags.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Agentic Opera Generator.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AgenticOperaGeneratorTest {

    @Test
    @IntegrationTest
    void testGenerateSingleScene() {
        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        Opera opera = generator.generateOpera("Test Agentic Opera", 1);

        assertThat(opera).isNotNull();
        assertThat(opera.title()).isEqualTo("Test Agentic Opera");
        assertThat(opera.scenes()).hasSize(1);
        assertThat(opera.scenes().getFirst().author()).isIn("GPT-5.2", "Claude Opus 4.6");
        assertThat(opera.scenes().getFirst().content()).isNotEmpty();

        System.out.println("✅ Generated agentic opera: " + opera.title());
        System.out.println("   Scene 1: " + opera.scenes().getFirst().title());
        System.out.println("   Author: " + opera.scenes().getFirst().author());
    }

    @Test
    @IntegrationTest
    void testGenerateTwoScenesWithAlternatingModels() {
        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        Opera opera = generator.generateOpera("Diversity Test Opera", 2);

        assertThat(opera).isNotNull();
        assertThat(opera.scenes()).hasSize(2);

        // Verify alternating models
        assertThat(opera.scenes().get(0).author()).isEqualTo("GPT-5.2");
        assertThat(opera.scenes().get(1).author()).isEqualTo("Claude Opus 4.6");

        System.out.println("✅ Verified model diversity:");
        opera.scenes().forEach(scene ->
                System.out.printf("   Scene %d by %s: %s%n",
                        scene.number(), scene.author(), scene.title()));
    }

    @Test
    @IntegrationTest
    void testAutoGenerateTitle() {
        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        Opera opera = generator.generateOpera(null, 1);

        assertThat(opera).isNotNull();
        assertThat(opera.title()).isNotEmpty();
        assertThat(opera.title()).isNotEqualTo("null");
        assertThat(opera.title()).isNotEqualTo("TITLE_GENERATION");

        System.out.println("✅ Auto-generated title: " + opera.title());
    }

    @Test
    @ExpensiveTest
    @EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
    void testFullProductionWorkflow() {
        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        // Generate a small opera
        Opera opera = generator.generateOpera("Agentic Production Test", 2);
        assertThat(opera.scenes()).hasSize(2);

        // Run the production workflow
        generator.runProductionWorkflow();

        // Verify outputs exist
        Path operaDir = generator.getTools().getOperaDirectory();
        assertThat(operaDir).isNotNull();
        assertThat(operaDir).exists();

        System.out.println("✅ Full production workflow complete");
        System.out.println("   Output directory: " + operaDir);
    }

    /**
     * Test the TRUE agentic mode where the supervisor autonomously
     * decides what to do based on a natural language request.
     * <p>
     * This demonstrates the key difference between:
     * - Manual orchestration: Code decides the sequence
     * - Agentic orchestration: LLM decides the sequence
     */
    @Test
    @ExpensiveTest
    @EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
    void testAutonomousSupervisorMode() {
        AgenticOperaGenerator generator = new AgenticOperaGenerator();

        // Give the supervisor a high-level request - it decides HOW to accomplish it
        String request = "Create a 2-scene opera about the relationship between humans and AI. " +
                "Make sure to alternate writing styles between scenes. " +
                "After the scenes are complete, save the libretto and generate illustrations.";

        System.out.println("═".repeat(60));
        System.out.println("  🧪 TESTING AUTONOMOUS SUPERVISOR MODE");
        System.out.println("═".repeat(60));
        System.out.println("Request: " + request);
        System.out.println();

        // The supervisor autonomously orchestrates everything
        String result = generator.generateOperaAutonomously(request);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        System.out.println();
        System.out.println("─".repeat(60));
        System.out.println("SUPERVISOR RESULT:");
        System.out.println("─".repeat(60));
        System.out.println(result);
        System.out.println();
        System.out.println("✅ Autonomous supervisor test complete");
    }
}
