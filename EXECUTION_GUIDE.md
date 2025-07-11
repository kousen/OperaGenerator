# Opera Generator - Execution Guide

## Overview

The Opera Generator is an automated system that uses AI models (GPT-4.1 and Claude Sonnet 4) to collaboratively create opera librettos and generate illustrations for each scene. This guide walks through the complete execution path and usage instructions.

## 🚀 Quick Start

### Basic Usage
```bash
# Generate opera with AI-suggested title and 5 scenes (default)
java IntegratedOperaGenerator

# Generate with custom title and scene count  
java IntegratedOperaGenerator "The Lost City of Hartford" 7

# Generate with custom title, default scene count
java IntegratedOperaGenerator "My Opera Title"
```

### Prerequisites
- Java 21 or higher
- Environment variables set for API keys:
  - `OPENAI_API_KEY` - For GPT-4.1 and DALL-E 3
  - `ANTHROPIC_API_KEY` - For Claude Sonnet 4

## 🔄 Complete Execution Path

### 1. **Main Method Entry** (`IntegratedOperaGenerator.main`)
```text
// Parse command line arguments
String operaTitle = args.length > 0 ? args[0] : null;  // Could be null
int numberOfScenes = args.length > 1 ? Integer.parseInt(args[1]) : 5;  // Default 5
```

### 2. **Opera Generation** (`Conversation.generateOpera`)
```text
Conversation conversation = new Conversation();
Opera opera = conversation.generateOpera(operaTitle, numberOfScenes);
```

**Process Details:**
- **2a.** If `title` is null/blank → Ask GPT-4.1 to suggest a title
- **2b.** Set up `ChatMemory` with the Connecticut jungle premise:
  ```
  Setting: Wild jungles of Connecticut after global warming  
  Characters: Soprano explorer, Tenor poet, Baritone government agent with robot  
  Plot: Classic opera love triangle with modern twist
  ```
- **2c.** Loop for `numberOfScenes` iterations:
  - **Alternates models:** `i % 2 == 0 ? gpt41 : claude`
  - **Sends prompt:** "Please write the next scene"
  - **Parses response** using regex pattern `Scene \d+: (.+)`
  - **Creates `Opera.Scene`** objects with (number, title, author, content)
  - **Adds to memory** for context continuity in next iteration
- **2d.** Returns complete `Opera` object with all scenes

### 3. **Libretto Saving** (`LibrettoWriter.saveLibretto`)
```text
var librettoPath = LibrettoWriter.saveLibretto(opera);
```

**Process Details:**
- **3a.** Creates filename: `{opera.title()}_libretto.md`
- **3b.** Builds markdown content:
  ```markdown
  # Opera Title
  
  ## Premise
  [premise text]
  
  ---
  
  ### Scene 1: Scene Title
  > **Author: GPT-4.1**
  
  [scene content]
  
  ---
  
  ### Scene 2: Another Scene
  > **Author: Claude Sonnet 4**
  
  [scene content]
  ```
- **3c.** Writes to `src/main/resources/{title}_libretto.md`

### 4. **Individual Scene Files** (`LibrettoWriter.saveScenesToFiles`)
```text
LibrettoWriter.saveScenesToFiles(opera);
```

**Process Details:**
- **4a.** For each scene in `opera.scenes()`:
- **4b.** Creates filename using `scene.getFileName()`: `scene_X_title.txt`
- **4c.** Writes formatted content:
  ```
  Scene X: Title
  Author: Model Name
  
  [scene content]
  ```

### 5. **Image Generation** (`OperaImageGenerator.generateImages`)
```text
OperaImageGenerator.generateImages(opera);
```

**Process Details:**
- **5a.** Creates DALL-E 3 model with API key
- **5b.** Opens virtual thread executor: `newVirtualThreadPerTaskExecutor()`
- **5c.** Maps each scene to `CompletableFuture.runAsync()`:
  - **Each virtual thread** calls `generateSingleImage(model, scene)`
  - **Concurrent execution** of all image generations
- **5d.** Waits for all futures: `CompletableFuture.allOf().join()`

**Inside `generateSingleImage(model, scene)`:**
- **5e.** Creates prompt: `"Create illustration for Scene X: Title. Description: [first 500 chars]"`
- **5f.** Calls `model.generate(prompt)` → DALL-E 3 API
- **5g.** Uses `ImageSaver.saveImage()` to download/save image
- **5h.** Renames to `scene.getImageFileName()`: `scene_X_illustration.png`

## 🏗️ Architecture & Design

### Key Classes

#### **Domain Models**
- **`Opera`** - Record containing title, premise, and list of scenes
- **`Opera.Scene`** - Nested record with number, title, author, content

#### **Core Components**
- **`IntegratedOperaGenerator`** - Main orchestration class and entry point
- **`Conversation`** - Orchestrates AI model alternation and scene generation
- **`LibrettoWriter`** - Handles Markdown and file output
- **`OperaImageGenerator`** - Manages concurrent image generation with gpt-image-1
- **`ImageSaver`** - Utility for downloading and saving images
- **`AiModels`** - Centralized AI model configuration
- **`ApiKeys`** - Environment variable access

#### **Java 21 Features Used**
- ✅ **Virtual threads** — Concurrent image generation with gpt-image-1
- ✅ **Records** — Domain modeling with Opera and Scene
- ✅ **Text blocks** — Multi-line string literals
- ✅ **Pattern matching** — Used in Conversation class for scene parsing

### Flow Control & Error Handling

**Virtual Thread Management:**
```text
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = opera.scenes().stream()
        .map(scene -> CompletableFuture.runAsync(() -> 
            generateSingleImage(model, scene), executor))
        .toList();
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    // Automatic cleanup when try-with-resources closes
}
```

## 📁 Output Structure

After successful execution, the following files are created in `src/main/resources/`:

```
src/main/resources/
├── {title}_libretto.md           # Complete opera markdown
├── scene_1_opening_scene.txt     # Individual scene files
├── scene_2_first_encounter.txt   
├── scene_3_love_duet.txt
├── scene_4_the_robot_appears.txt
├── scene_5_final_confrontation.txt
├── scene_1_illustration.png      # Generated illustrations
├── scene_2_illustration.png
├── scene_3_illustration.png
├── scene_4_illustration.png
└── scene_5_illustration.png
```

## 🎭 Key Data Flow

1. **Command Line Args** → **Conversation Class** → **Opera Object**
2. **Opera Object** → **LibrettoWriter** → **Markdown + Individual Scene Files**  
3. **Opera Object** → **OperaImageGenerator** → **Concurrent Image Generation**
4. **All Virtual Threads Complete** → **Summary Output**

## ⚡ Performance Features

- **Concurrent Image Generation:** Virtual threads allow multiple images to be generated simultaneously
- **Memory Management:** ChatMemory maintains context between scene generations
- **Error Recovery:** Graceful handling of API failures and parsing errors
- **Resource Cleanup:** Automatic cleanup of virtual thread executors

## 🔧 Alternative Usage

### Using Individual Components

```text
// Generate opera scenes only
Conversation conv = new Conversation();
Opera opera = conv.generateOpera("My Title", 3);

// Save libretto only
LibrettoWriter.saveLibretto(opera);

// Generate images only (requires existing Opera object)
OperaImageGenerator.generateImages(opera);
```

## 🛠️ Troubleshooting

### Common Issues

1. **Missing API Keys:** Ensure environment variables are set
2. **Invalid Scene Count:** Must be a positive integer
3. **Network Issues:** Image generation may fail if OpenAI API is unreachable
4. **File Permissions:** Ensure write access to `src/main/resources/`

### Error Messages

- `❌ No scene files found` — Run IntegratedOperaGenerator first
- `❌ Invalid number of scenes` — Check command line argument format
- `❌ Error during opera generation` — Check API keys and network connectivity

---

*The beauty of this system is the elimination of manual steps — everything flows automatically from AI conversation to final illustrated opera!*