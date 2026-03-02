package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.kousenit.tags.IntegrationTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "GOOGLEAI_API_KEY", matches = ".+")
@IntegrationTest
class OperaCriticTest {

    @Test
    void testCritiqueVinesOfReckoningOpera() throws Exception {
        // Given
        OperaCritic critic = new OperaCritic();
        Path operaDir = Path.of("src/main/resources/the_vines_of_reckoning_a_connecticut_jungle_opera");
        String operaTitle = "The Vines of Reckoning: A Connecticut Jungle Opera";

        // When
        System.out.println("Generating critique for The Vines of Reckoning...\n");
        critic.reviewAndSave(operaDir, operaTitle);

        // Then
        Path critiquePath = operaDir.resolve("the_vines_of_reckoning_a_connecticut_jungle_opera_critique.md");
        assertThat(critiquePath).exists();

        System.out.println("\nCritique successfully generated and saved!");
    }

    @Test
    void testCritiqueHartfordOpera() throws Exception {
        // Given
        OperaCritic critic = new OperaCritic();
        Path operaDir = Path.of("src/main/resources/hartford_ascending_an_opera_of_love_and_ruins");
        String operaTitle = "Hartford Ascending: An Opera of Love and Ruins";
        
        // When
        System.out.println("🎭 Testing opera critique generation...\n");
        critic.reviewAndSave(operaDir, operaTitle);
        
        // Then
        Path critiquePath = operaDir.resolve("hartford_ascending_an_opera_of_love_and_ruins_critique.md");
        assertThat(critiquePath).exists();
        
        System.out.println("\n✅ Critique successfully generated and saved!");
    }
}