# Claude Assistant Context

This document provides context for Claude when working on the Opera Generator project.

## Project Overview

This is an AI-powered opera generation system built with Java 21 and LangChain4j 1.1.0. The system creates complete multimedia operas by:
1. Using GPT-4.1 and Claude Sonnet 4 to alternately write scenes
2. Generating illustrations with OpenAI's gpt-image-1 model
3. Creating dramatic voice narration with ElevenLabs text-to-speech
4. Playing audio with JLayer for live demonstrations
5. Creating formatted libretti with embedded images and automatic stanza formatting

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
- **ElevenLabs**: Uses HttpClient for API calls, streams audio directly to files
- **Voice IDs**: Bella (narrator) and Antoni (critic) for different character voices

### Project Structure
```
src/main/java/com/kousenit/
├── IntegratedOperaGenerator.java  # Main entry point
├── Opera.java                     # Domain model (Opera and Scene records)
├── Conversation.java              # AI scene generation logic
├── LibrettoWriter.java           # File output handling with automatic formatting
├── OperaImageGenerator.java      # Image generation with rate limiting
├── ImageSaver.java               # Handles base64 and URL image saving
├── NarratorVoice.java            # ElevenLabs voice narration
├── AudioPlayer.java              # JLayer audio playback
├── AiModels.java                 # Model configurations
├── ApiKeys.java                  # Environment variable access
└── OperaCritic.java             # Generates critical reviews using Gemini
```

### Workflow

1. **IntegratedOperaGenerator** orchestrates the entire process
2. **Conversation** generates scenes alternating between AI models
3. **LibrettoWriter** saves markdown and individual scene files with automatic formatting
4. **OperaImageGenerator** creates illustrations with rate limiting
5. **NarratorVoice** generates dramatic audio narration using ElevenLabs
6. **AudioPlayer** plays generated audio for live demonstrations
7. **OperaCritic** (optional) generates critical review using Gemini

### Testing Commands

```bash
# Run main opera generation
./gradlew run

# Run specific tests
./gradlew test --tests ImageModelTest
./gradlew test --tests OperaGenerationIntegrationTest

# Audio generation and playback (requires ELEVENLABS_API_KEY)
./gradlew test --tests AudioDemoTest::generateAndPlayOperaIntroduction
./gradlew test --tests NarratorVoiceTest

# Compile and run main class directly
./gradlew compileJava
java -cp build/classes/java/main com.kousenit.IntegratedOperaGenerator
```

### Known Issues and Solutions

1. **gpt-image-1 timeouts**: Already handled with 3-minute timeout
2. **Rate limiting**: Implemented with Semaphore (max 2 concurrent) and delays
3. **Generic titles**: Improved prompt to request creative opera titles
4. **Incomplete operas**: ✅ SOLVED - Created continuation system with `ContinueHartfordOperaTest`
5. **Markdown stanza formatting**: ✅ SOLVED - Implemented automatic formatting in LibrettoWriter

### Current State (COMPLETE PROJECT)

✅ **"Hartford Ascending: An Opera of Love and Ruins" - COMPLETE 8-Scene Opera**:
- **Scenes 1-5**: Original generation (complete)
- **Scenes 6-8**: Continuation successfully generated and merged
- **All 8 illustrations**: Generated and properly organized
- **Complete libretto**: Fully merged with automatic stanza formatting
- **Critical review**: Generated by Gemini AI critic
- **Automatic formatting**: All new operas now get beautiful stanza formatting automatically

**File Structure**:
```
src/main/resources/hartford_ascending_an_opera_of_love_and_ruins/
├── hartford_ascending_an_opera_of_love_and_ruins_complete_libretto.md (8 scenes)
├── hartford_ascending_an_opera_of_love_and_ruins_critique.md
├── scene_1_encounter_beneath_the_banyan_trees.txt + scene_1_illustration.png
├── scene_2_the_agent's_oath.txt + scene_2_illustration.png  
├── scene_3_heartbeats_and_warning_calls.txt + scene_3_illustration.png
├── scene_4_the_mechanical_pursuit.txt + scene_4_illustration.png
├── scene_5_among_echoes_and_ruins.txt + scene_5_illustration.png
├── scene_6_the_gates_of_awakening.txt + scene_6_illustration.png
├── scene_7_the_heart_of_hartford.txt + scene_7_illustration.png
└── scene_8_finale_–_the_new_dawn.txt + scene_8_illustration.png
```

### Opera Completion Workflow

**Created successful pattern for continuing unfinished operas**:
1. `ContinueHartfordOperaTest.java` - Template for opera continuation
2. Proper scene numbering (6, 7, 8) with context from previous scenes
3. Image generation for new scenes with rate limiting
4. Successful merger into main opera directory
5. Complete libretto integration

### Automatic Libretto Formatting System

**Implemented automatic stanza formatting in LibrettoWriter.java**:
- **formatSceneContent()**: Automatically detects character lines and formats lyrics
- **determineVoiceType()**: Intelligently assigns voice types based on character names
- **Pattern**: `CHARACTER (description): Line one, Line two` → `**CHARACTER** (voice type): > Line one<br> > Line two`
- **Status**: ✅ COMPLETE - All new operas get beautiful formatting automatically
- **Location**: `LibrettoWriter.java:25-111` contains the full implementation

### Next Steps for New Work

When continuing development:
1. ✅ Opera is complete - can use as reference for new operas
2. ✅ Continuation pattern established - use `ContinueHartfordOperaTest` as template
3. ✅ Automatic formatting implemented - all new operas will be beautifully formatted
4. Consider: Generate new operas with different themes/settings
5. Consider: Test the automatic formatting with various character names and lyric styles

### Environment Requirements

Must have these environment variables set:
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `GOOGLEAI_API_KEY` (only for critique functionality)
- `ELEVENLABS_API_KEY` (only for voice narration)

### Useful Patterns

```java
// Rate-limited image generation
OperaImageGenerator.generateImages(opera);

// Generate opera with custom parameters
Opera opera = conversation.generateOpera(title, numberOfScenes);

// Generate and play audio narration
NarratorVoice narrator = new NarratorVoice();
Path audioFile = narrator.generateOperaIntroduction(opera, outputDir);
AudioPlayer.play(audioFile);

// Continue an opera (from ContinueHartfordOperaTest pattern)
String continuationContext = premise + """
    IMPORTANT CONTINUATION CONTEXT:
    The opera has already progressed through 5 scenes...
    Continue from Scene 6. You must complete the opera in 3 scenes...
    """;
Opera continuation = conversation.generateOpera(title, continuationContext, 3);

// HttpClient pattern for ElevenLabs integration
var request = HttpRequest.newBuilder()
    .uri(URI.create(API_URL + voiceId))
    .header("xi-api-key", ELEVENLABS_API_KEY)
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .build();

// Stream directly to file - efficient for large audio files
var response = client.send(request, HttpResponse.BodyHandlers.ofFile(outputPath));
```

### Continuation Test Pattern

When creating continuation tests, follow this proven pattern:
1. Read existing libretto to extract premise
2. Create continuation context with story summary
3. Generate new scenes with proper numbering (startingSceneNumber + i)
4. Save to separate continuation directory first
5. Generate images for new scenes
6. Copy/move files to main opera directory
7. Update complete libretto with new scenes

### Automatic Formatting Implementation

The `formatSceneContent()` method automatically:
1. Detects character lines using regex pattern `SINGING_PATTERN`
2. Determines voice types using `determineVoiceType()` method
3. Converts lyrics to blockquotes with `<br>` tags
4. Preserves stage directions `[...]` outside formatting
5. Handles various character name patterns (SANDRA, LUCIAN, etc.)

**Key Methods in LibrettoWriter.java**:
- `formatSceneContent(String content)` - Main formatting logic
- `determineVoiceType(String character)` - Voice type detection
- `saveCompleteOpera(Opera opera)` - Saves with automatic formatting

**Key Methods in NarratorVoice.java**:
- `generateSceneNarration(Scene scene, Path outputDir)` - Extracts stage directions and creates audio
- `generateOperaIntroduction(Opera opera, Path outputDir)` - Creates dramatic introduction
- `generateAudio(String text, String voiceId, Path outputPath)` - Core ElevenLabs integration

**Key Methods in AudioPlayer.java**:
- `play(Path audioFile)` - Blocking audio playback with JLayer
- `playAsync(Path audioFile)` - Non-blocking playback using virtual threads