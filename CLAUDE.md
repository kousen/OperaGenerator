# Claude Assistant Context

This document provides context for Claude when working on the Opera Generator project.

## Project Overview

This is an AI-powered opera generation system built with Java 21 and LangChain4j 1.1.0. The system creates complete operas by:
1. Using GPT-4.1 and Claude Sonnet 4 to alternately write scenes
2. Generating illustrations with OpenAI's gpt-image-1 model
3. Creating formatted libretti with embedded images

## Key Technical Details

### Java 21 Features Used
- **Virtual threads** for concurrent image generation
- **Records** for domain modeling (Opera and Scene)
- **Pattern matching** in switch expressions
- **Text blocks** for multi-line strings

### Important Model Information
- **gpt-image-1**: Returns base64-encoded images (not URLs), no revised prompt, no token usage info
- **Image generation timeout**: Set to 3 minutes due to slow response times
- **Rate limiting**: Max 2 concurrent image requests with 1-second delays to avoid API throttling

### Project Structure
```
src/main/java/com/kousenit/
├── IntegratedOperaGenerator.java  # Main entry point
├── Opera.java                     # Domain model (Opera and Scene records)
├── Conversation.java              # AI scene generation logic
├── LibrettoWriter.java           # File output handling
├── OperaImageGenerator.java      # Image generation with rate limiting
├── ImageSaver.java               # Handles base64 and URL image saving
├── AiModels.java                 # Model configurations
├── ApiKeys.java                  # Environment variable access
└── OperaOrganizer.java          # Utility for organizing completed operas
```

### Workflow

1. **IntegratedOperaGenerator** orchestrates the entire process
2. **Conversation** generates scenes alternating between AI models
3. **LibrettoWriter** saves markdown and individual scene files
4. **OperaImageGenerator** creates illustrations with rate limiting
5. **OperaOrganizer** can reorganize files and create complete libretti

### Testing Commands

```bash
# Run main opera generation
./gradlew run

# Run specific tests
./gradlew test --tests ImageModelTest
./gradlew test --tests OperaGenerationIntegrationTest

# Compile and run main class directly
./gradlew compileJava
java -cp build/classes/java/main com.kousenit.IntegratedOperaGenerator
```

### Known Issues and Solutions

1. **gpt-image-1 timeouts**: Already handled with 3-minute timeout
2. **Rate limiting**: Implemented with Semaphore (max 2 concurrent) and delays
3. **Generic titles**: Improved prompt to request creative opera titles
4. **Incomplete operas**: Current implementation stops at Scene 5, may need continuation

### Current State

- Successfully generated "Hartford Ascending: An Opera of Love and Ruins"
- Opera ends on cliffhanger at Scene 5 (needs 2-3 more scenes for completion)
- All files organized in `src/main/resources/hartford_ascending_an_opera_of_love_and_ruins/`
- Complete libretto with embedded images created

### Next Steps

When continuing work:
1. Check if user wants to complete the unfinished opera
2. Remember rate limiting is crucial for image generation
3. Use OperaOrganizer to reorganize any new operas
4. Test image generation separately if needed using ImageModelTest

### Environment Requirements

Must have these environment variables set:
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `GOOGLEAI_API_KEY` (only for critique functionality)

### Useful Patterns

```java
// Rate-limited image generation
OperaImageGenerator.generateImages(opera, maxConcurrent, delayBetween);

// Organize completed opera
OperaOrganizer.organizeOpera("current_title", "New Title");

// Generate opera with custom parameters
Opera opera = conversation.generateOpera(title, numberOfScenes);
```