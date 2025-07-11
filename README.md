# Opera Generator

An AI-powered opera generation system that creates complete libretti with illustrations using OpenAI's GPT-4.1, Anthropic's Claude Sonnet 4, and OpenAI's gpt-image-1 model.

## Overview

This application generates original operas by:
- Alternating between GPT-4.1 and Claude Sonnet 4 to write scenes
- Automatically generating illustrations for each scene using gpt-image-1
- Creating properly formatted libretti with embedded images
- Organizing all assets into a structured directory

## Prerequisites

- Java 21 or higher
- Gradle
- API keys for:
  - OpenAI (for GPT-4.1 and image generation)
  - Anthropic (for Claude Sonnet 4)

## Setup

1. Set environment variables for API access:
   ```bash
   export OPENAI_API_KEY=your_openai_key
   export ANTHROPIC_API_KEY=your_anthropic_key
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

### What Gets Generated

The system creates:
1. **A complete libretto** in Markdown format with all scenes
2. **Individual scene files** with detailed stage directions and lyrics
3. **AI-generated illustrations** for each scene (PNG format)
4. **An organized directory** containing all assets

Example output structure:
```
src/main/resources/opera_title/
├── opera_title_complete_libretto.md   # Full opera with embedded images
├── scene_1_title.txt                   # Individual scene files
├── scene_1_illustration.png            # AI-generated illustrations
├── scene_2_title.txt
├── scene_2_illustration.png
└── ...
```

## Features

- **AI Collaboration**: GPT-4.1 and Claude alternate writing scenes, creating unique stylistic variety
- **Visual Storytelling**: Each scene gets an AI-generated illustration based on its content
- **Professional Formatting**: Proper opera formatting with stage directions, character names, and sung lyrics
- **Rate Limiting**: Intelligent throttling prevents API rate limit issues
- **Modern Java**: Uses Java 21 features including virtual threads for efficient concurrent processing

## Example Opera

The system can generate operas on any theme. The default premise involves:
- A post-climate change Connecticut transformed into jungle
- A soprano explorer searching for the lost city of Hartford
- A tenor poet living in the wilderness
- A baritone government agent with a robot that sings opera

## Performance

- Scene generation: ~30 seconds per scene
- Image generation: ~1 minute per image (with rate limiting)
- Total time for 5-scene opera: ~8-10 minutes

## Architecture

Key components:
- `IntegratedOperaGenerator` - Main orchestration class
- `Conversation` - Manages AI model interactions
- `OperaImageGenerator` - Handles illustration generation
- `LibrettoWriter` - Formats and saves opera content
- `Opera` - Domain model using Java records

## Troubleshooting

- **Timeout errors**: The system includes 3-minute timeouts for image generation
- **Rate limiting**: Images are generated with controlled concurrency (max 2 at a time)
- **Missing API keys**: Ensure environment variables are set before running

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.