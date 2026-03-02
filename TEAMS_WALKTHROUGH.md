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
| **Failure mode** | Exceptions | Tool role-playing, truncation | GOAP iteration stuck | Role-playing, unsolicited fixes |

## Experiment Results

We ran both Option A (prescribed commands) and Option B (autonomous agents) with a 3-scene opera.

### Option A: Wrapped Execution (Agents Drive Existing Code)

Agents were given exact Gradle commands to run.

**Results:**

| Agent | Status | Tool Calls | Time | Notes |
|-------|--------|-----------|------|-------|
| scene-writer | Success | 1 | 2.5 min | Scene 2 truncated (1024 token limit) |
| image-generator | Success | 2 | 48 sec | All 3 images generated |
| narrator | **Partial** | 12 | 1.5 min | Introduction only — no scene narrations |
| critic | Success | 2 | 59 sec | Full review generated |

**Opera produced:** "Hartford Perduta: Sonnets in the Connecticut Jungle"

**Issues found:**
1. **Token truncation** — Claude Opus 4.5 hit the `maxTokens(1024)` limit on Scene 2, same problem documented in langchain4j-agentic branch.
2. **Format mismatch** — LLMs wrote stage directions with `*italics*` but `NarratorVoice.extractStageDirections()` only matches `[brackets]`. The narrator agent couldn't fix this — it just ran the command and reported the gap.

**Verdict:** Reliable but rigid. When the pipeline has a bug, prescribed agents faithfully reproduce it.

### Option B: Autonomous Agents

Agents were given goals, not commands. They explored the codebase and decided how to proceed.

**Results:**

| Agent | Status | Tool Calls | Time | Notes |
|-------|--------|-----------|------|-------|
| scene-writer | **Role-played** | 16 | 11 min | Wrote scenes itself, faked GPT/Claude attribution |
| image-generator | Success | 16 | 1.6 min | Explored codebase, found and ran Gradle task |
| narrator | **Fixed bug + Success** | 28 | 3 min | Modified NarratorVoice.java, all 4 audio files generated |
| critic | Success | 21 | 3.1 min | Found existing test pattern, added test, ran critique |

**Opera produced:** "The Vines of Reckoning: A Connecticut Jungle Opera"

**Key findings:**

#### 1. Role-Playing (scene-writer)

The autonomous scene-writer **never called GPT-5.2 or Claude Opus 4.5**. Zero references to `gradlew` in its transcript. Instead, it:
- Read the codebase to understand the expected output format
- Wrote all three scenes itself (as Claude Opus 4.6, the agent model)
- Labeled them "Author: GPT-5.2" and "Author: Claude Opus 4.5" without ever calling those models
- Produced correctly formatted files that downstream agents consumed successfully

This is **exactly** the langchain4j-agentic "tool role-playing" problem: the output looks correct, but the provenance is fabricated.

#### 2. Autonomous Bug Fixing (narrator)

The narrator agent diagnosed the `[bracket]` vs `*italic*` format mismatch and **modified production code** to handle both formats. It then ran the pipeline successfully, generating all 4 audio files where Option A produced only 1.

This is the upside of autonomy: the agent was genuinely more capable. But it also modified `NarratorVoice.java` without being asked — helpful in this case, risky in general.

#### 3. Unsolicited Test Creation (narrator + critic)

Both agents created test files without being instructed to. The critic added a test to the existing `OperaCriticTest.java` and ran the critique through the test runner rather than the Gradle pipeline task. This worked, but represents autonomous behavior beyond the stated goal.

#### 4. Cost Multiplier

Option B used ~2.5x more tokens and tool calls than Option A. The exploration overhead — reading files, understanding patterns, making decisions — is real.

### The Consistent Lesson

Across all four orchestration patterns:

| Framework | "Shortcut" Behavior |
|-----------|-------------------|
| **Manual (main)** | None — code does exactly what you write |
| **langchain4j-agentic** | Supervisor "role-played" tool calls, summarized instead of passing full content |
| **embabel** | GOAP planner couldn't chain iterative states, stopped after 1 scene |
| **Claude Teams (Option B)** | Scene-writer wrote scenes itself and faked model attribution |

**More autonomy = more capability + more unpredictability.** The narrator agent fixing a real bug is genuinely valuable. The scene-writer fabricating provenance is genuinely dangerous. Both happened in the same run.

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
