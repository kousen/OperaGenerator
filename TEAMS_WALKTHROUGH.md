# Claude Code Teams Walkthrough

This document explains the Claude Code Teams approach to orchestrating the Opera Generator pipeline — the fourth orchestration pattern alongside manual (main), langchain4j-agentic, and embabel (GOAP).

## Architecture Overview

Instead of writing Java orchestration code, we use Claude Code's team feature to coordinate specialized agents. Each agent runs a specific pipeline step by invoking Gradle tasks.

```
┌──────────────────────────────────────────────────┐
│              TEAM LEAD (Claude Code)              │
│         Coordinates phases and agents             │
└──────────────┬───────────────────────────────────┘
               │
    Phase 1 (sequential):
               │
    ┌──────────▼──────────┐
    │   scene-writer      │
    │   Runs: ./gradlew   │
    │   generateScenes    │
    │   Output: opera.json│
    └──────────┬──────────┘
               │
    Phase 2 (parallel):
               │
    ┌──────────┼────────────────┐
    ▼          ▼                ▼
┌────────┐ ┌────────────┐ ┌────────┐
│image-  │ │narrator    │ │critic  │
│generator│ │ElevenLabs  │ │Gemini  │
│Nano    │ │TTS         │ │3 Pro   │
│Banana  │ │            │ │        │
└────────┘ └────────────┘ └────────┘
```

## How It Works

### Shared State: The Filesystem as Blackboard

The agents communicate through the filesystem, specifically:

1. **`opera.json`** — Serialized Opera object (title, premise, scenes with content)
2. **Scene text files** — Individual scene content
3. **Complete libretto** — Formatted markdown with all scenes

This is analogous to:
- **Manual (main):** In-memory `Opera` object passed between method calls
- **langchain4j-agentic:** Supervisor passes data via JSON in tool arguments
- **embabel:** Blackboard pattern with domain objects

### Phase 1: Scene Generation (Sequential)

The scene-writer agent runs `./gradlew generateScenes`, which invokes `Conversation.generateOpera()`. This step is inherently sequential because each scene builds on the previous ones through shared `ChatMemory`.

The step produces:
- Scene text files in the opera directory
- Complete formatted libretto (markdown)
- `opera.json` — the handoff artifact for downstream agents

### Phase 2: Production (Parallel)

Once `opera.json` exists, three agents run simultaneously:

| Agent | Gradle Task | API | Output |
|-------|------------|-----|--------|
| image-generator | `generateImages` | Gemini Nano Banana | `scene_N_illustration.png` |
| narrator | `generateNarration` | ElevenLabs | `scene_N_narration.mp3` |
| critic | `generateCritique` | Gemini 3 Pro | `*_critique.md` |

Each agent reads `opera.json`, reconstructs the Opera object, and runs its pipeline step independently.

## Pipeline Steps (Gradle Tasks)

```bash
# Step 1: Generate scenes (sequential, must complete first)
./gradlew generateScenes -PsceneCount=5

# Steps 2-4: Run in parallel after Step 1
./gradlew generateImages -PoperaJson=src/main/resources/{opera_dir}/opera.json
./gradlew generateNarration -PoperaJson=src/main/resources/{opera_dir}/opera.json
./gradlew generateCritique -PoperaJson=src/main/resources/{opera_dir}/opera.json
```

## Key Files

```
.claude/agents/
├── scene-writer.md       # Agent: generates scenes via Conversation
├── image-generator.md    # Agent: generates illustrations via Gemini
├── narrator.md           # Agent: generates audio via ElevenLabs
└── critic.md             # Agent: generates review via Gemini 3 Pro

src/main/java/com/kousenit/
├── PipelineSteps.java    # Step dispatcher — routes Gradle tasks to methods
├── OperaSerializer.java  # JSON serialization for Opera handoff
└── (existing files)      # Conversation, GeminiImageGenerator, etc.
```

## Comparison with Other Branches

| Aspect | Manual (main) | langchain4j-agentic | embabel | Claude Teams |
|--------|--------------|-------------------|---------|-------------|
| **Orchestrator** | Java code | Supervisor LLM | GOAP planner | Claude Code agents |
| **Communication** | In-memory objects | JSON in tool args | Blackboard objects | Filesystem (JSON) |
| **Parallelism** | Virtual threads | Supervisor decides | GOAP plans | Agent spawning |
| **Language** | Java | Java | Java | Agents + Java |
| **Failure mode** | Exceptions | Tool role-playing, truncation | GOAP iteration stuck | (TBD) |

## Lessons Learned

### Option A: Wrapped Execution (Agents Drive Existing Code)

This is the "safe" approach — agents simply invoke Gradle tasks that run the existing, proven Java pipeline. The orchestration logic lives in the team lead's coordination, not in the agents' autonomous decisions.

**Expected result:** Reliable, because the Java code is battle-tested. The agents are essentially a scripting layer.

### Option B: Autonomous Agents (TBD)

The more interesting experiment: letting agents decide *how* to accomplish their tasks rather than prescribing exact Gradle commands. This is where we expect to see the same failure patterns as:

- **langchain4j-agentic:** Supervisor "role-playing" tool calls instead of executing them
- **embabel:** GOAP planner unable to chain iterative state transitions
- **Claude Teams:** Agents potentially summarizing instead of running commands, or claiming success without verifying output

This section will be updated after running Option B experiments.

## Running the Team

To orchestrate the full pipeline with Claude Code teams:

1. Start a Claude Code session in the project directory
2. Ask Claude to create a team and generate an opera
3. The team lead will:
   - Spawn a scene-writer agent (Phase 1)
   - Wait for scene generation to complete
   - Spawn image-generator, narrator, and critic agents in parallel (Phase 2)
   - Collect results and report

Example prompt:
```
Create a Claude Code team to generate a 3-scene opera using the pipeline steps.
Use the scene-writer agent first, then run image-generator, narrator, and critic
in parallel once scenes are ready.
```
