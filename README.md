# Opera Generator

An AI-powered opera generation system that orchestrates multiple AI models to create complete multimedia operas with text, images, narration, and music.

## Overview

This application generates original operas by:
- **Text Generation**: Alternating between GPT-4.1 and Claude Sonnet 4 to write scenes
- **Image Generation**: Creating illustrations for each scene using OpenAI's gpt-image-1
- **Voice Narration**: Generating dramatic audio using ElevenLabs text-to-speech
- **Audio Playback**: Playing generated narration with JLayer
- **Automatic Formatting**: Beautiful stanza formatting applied when scenes are saved
- **Critical Reviews**: AI-generated critiques using Google Gemini
- **Complete Organization**: All assets structured in organized directories

Plus integration with external tools:
- **Suno AI**: For actual opera music composition
- **NotebookLM**: For AI-generated podcasts about the opera

## Prerequisites

- Java 21 or higher
- Gradle
- API keys for:
  - OpenAI (for GPT-4.1 and gpt-image-1 image generation)
  - Anthropic (for Claude Sonnet 4)
  - Google AI (optional, for opera critique feature)
  - ElevenLabs (optional, for voice narration)

## Setup

1. Set environment variables for API access:
   ```bash
   export OPENAI_API_KEY=your_openai_key
   export ANTHROPIC_API_KEY=your_anthropic_key
   export GOOGLEAI_API_KEY=your_google_ai_key    # Optional, for critique feature
   export ELEVENLABS_API_KEY=your_elevenlabs_key # Optional, for voice narration
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

## Usage

### Generate a Complete Opera

Run the integrated opera generator:

```bash
# Generate with auto-generated title and 5 scenes (default)
java -cp build/classes/java/main com.kousenit.IntegratedOperaGenerator

# Generate with custom title and scene count
java -cp build/classes/java/main com.kousenit.IntegratedOperaGenerator "My Opera Title" 7
```

### Generate Opera Critique

Use the opera critic to review your completed libretto:

```bash
# Run the critic test (requires GOOGLEAI_API_KEY)
./gradlew test --tests OperaCriticTest
```

### Generate and Play Voice Narration

Create and play dramatic AI narration for your opera:

```bash
# Generate and play opera introduction (requires ELEVENLABS_API_KEY)
./gradlew test --tests AudioDemoTest::generateAndPlayOperaIntroduction

# Generate and play scene narration
./gradlew test --tests AudioDemoTest::generateAndPlaySceneNarration

# Generate narration files only (no playback)
./gradlew test --tests NarratorVoiceTest
```

### Continue an Unfinished Opera

For operas that need additional scenes to complete their story:

```bash
# Run the continuation test
./gradlew test --tests ContinueHartfordOperaTest
```

### What Gets Generated

The system creates:
1. **A complete libretto** in Markdown format with all scenes
2. **Individual scene files** with detailed stage directions and lyrics
3. **AI-generated illustrations** for each scene (PNG format)
4. **Voice narration files** for dramatic stage directions (MP3 format)
5. **An organized directory** containing all assets

Example output structure:
```
src/main/resources/opera_title/
├── opera_title_complete_libretto.md   # Full opera with embedded images
├── scene_1_title.txt                   # Individual scene files
├── scene_1_illustration.png            # AI-generated illustrations
├── scene_1_narration.mp3               # AI voice narration
├── scene_2_title.txt
├── scene_2_illustration.png
├── scene_2_narration.mp3
├── opera_introduction.mp3              # Dramatic introduction
└── ...
```

## Features

- **AI Collaboration**: GPT-4.1 and Claude alternate writing scenes, creating unique stylistic variety
- **Visual Storytelling**: Each scene gets an AI-generated illustration using gpt-image-1
- **Voice Narration**: Dramatic audio narration of stage directions using ElevenLabs
- **Audio Playback**: Live audio playback using JLayer for presentations and demos
- **Automatic Formatting**: Beautiful stanza formatting applied automatically when scenes are saved
- **Professional Layout**: Proper opera formatting with stage directions, character names, and sung lyrics
- **Critical Review**: Optional AI-generated critique by Google Gemini acting as an opera critic
- **Rate Limiting**: Intelligent throttling prevents API rate limit issues
- **Modern Java**: Uses Java 21 features including virtual threads, HttpClient, and records

## Featured Opera: "Hartford Ascending: An Opera of Love and Ruins"

The project includes a complete 8-scene opera with beautiful libretto formatting:

**Story**: Set in post-climate change Connecticut where jungle has reclaimed the land
- **Sandra** (soprano): An explorer seeking the lost city of Hartford  
- **Lucian** (tenor): A jungle poet who falls in love with Sandra
- **Maximilian** (baritone): A government agent trying to stop them
- **Aria-7 Robot** (bass): Maximilian's AI that sings opera in multiple languages

**Highlights**:
- Complete narrative arc from meeting to triumphant finale
- Stunning AI-generated illustrations for all 8 scenes using gpt-image-1
- Professional libretto formatting with proper stanza display
- Dramatic AI narration for stage directions
- Actual opera music composed with Suno AI
- Critical review praising its "fearless embrace of the bizarre"
- Themes of love, nature, technology, and transformation

**Location**: `src/main/resources/hartford_ascending_an_opera_of_love_and_ruins/`

## Performance

- Scene generation: ~30 seconds per scene
- Image generation: ~1 minute per image (with rate limiting)
- Voice narration: ~10-15 seconds per scene
- Total time for 5-scene opera: ~8-10 minutes (plus ~1 minute for audio)

## Architecture

Key components:
- `IntegratedOperaGenerator` - Main orchestration class
- `Conversation` - Manages AI model interactions
- `OperaImageGenerator` - Handles illustration generation with rate limiting
- `LibrettoWriter` - Formats and saves opera content with automatic stanza formatting
- `NarratorVoice` - Generates dramatic audio narration using ElevenLabs
- `AudioPlayer` - Plays generated audio using JLayer
- `OperaCritic` - Generates critical reviews using Google Gemini
- `ContinueHartfordOperaTest` - Example of continuing unfinished operas
- `Opera` - Domain model using Java records with nested Scene records

## Troubleshooting

- **Timeout errors**: The system includes 3-minute timeouts for image generation
- **Rate limiting**: Images are generated with controlled concurrency (max 2 at a time)
- **Missing API keys**: Ensure environment variables are set before running
- **Audio playback issues**: JLayer requires proper audio system setup
- **ElevenLabs errors**: Check API key and voice ID configuration
- **Formatting issues**: Stanza formatting is now automatic - no manual intervention needed
- **Duplicate scenes**: Check continuation directory if you see duplicate files

## Documentation

- **`CLAUDE.md`** - Technical context for AI assistant development
- **`EXECUTION_GUIDE.md`** - Step-by-step execution instructions

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.