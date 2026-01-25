# Slides Migration Notes: From "AI Orchestra" to "Multi-Agent Coding"

This document outlines changes needed to update `slides.md` for a presentation focused on langchain4j-agentic and multi-agent orchestration.

---

## Current Slides Focus (AI Orchestra Pattern)

The existing slides emphasize:
- Multiple models alternating (GPT + Claude writing scenes)
- Shared memory via `ChatMemory`
- Various AI integrations (images, voice, music, critic)
- Modern Java features (records, virtual threads)
- The complete "orchestra" of AI tools

---

## New Focus: Multi-Agent Orchestration

The new presentation should emphasize:
- **Supervisor pattern** (autonomous decision-making)
- **langchain4j-agentic** framework specifics
- **Lessons learned** from the porting process
- **Manual vs Agentic** tradeoffs
- Still showcase the Java + LangChain4j integration

---

## Slide-by-Slide Changes

### Title & Opening (Slides 1-2)
**Keep:** Title, contact info, GitHub link
**Change:** Subtitle from "Multiple LLMs in Concert" to something like "Building Multi-Agent Systems in Java" or "Autonomous AI Orchestration with langchain4j-agentic"

### The Premise (Slide 3)
**Keep:** The absurd premise is still relevant - it's what makes the demo memorable
**Consider:** Mentioning that this same premise now powers both manual and agentic modes

### The AI Orchestra (Slide 4)
**Update:**
- Model names: GPT-5 → GPT-5.2, Claude Opus 4.1 → Claude Opus 4.5
- Image generation: gpt-image-1 → Gemini Nano Banana
- Add mention of **supervisor agent** orchestrating everything
- Code snippet should show the agentic approach:
```java
// Agentic - supervisor decides everything
String result = generator.generateOperaAutonomously(2);

// vs Manual - code controls sequence
Opera opera = generator.generateOpera(title, numberOfScenes);
generator.runProductionWorkflow();
```

### Lesson 1: Orchestrating Multiple LLMs (Slides 5-10)
**Major Rewrite Needed**

Replace the "alternate between models" approach with:

**NEW: The Supervisor Pattern**
```
┌─────────────────────────────────────┐
│     SUPERVISOR AGENT                │
│     (Claude Opus 4.5 - 8192 tokens) │
└──────────────┬──────────────────────┘
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌──────────────┐
│GPT     │ │Claude  │ │Production    │
│Writer  │ │Writer  │ │Agent (tools) │
└────────┘ └────────┘ └──────────────┘
```

**NEW: AgenticServices Builder Pattern**
```java
// Define agent interface
@Agent(description = "Writes opera scenes...")
public interface SceneWriterAgent {
    @UserMessage("Write Scene {{sceneNumber}}...")
    String writeScene(...);
}

// Build agents with AgenticServices
this.gptWriter = AgenticServices
    .agentBuilder(SceneWriterAgent.class)
    .chatModel(AiModels.GPT_5_2)
    .name("gptSceneWriter")
    .build();

// Build supervisor
this.supervisor = AgenticServices.supervisorBuilder()
    .chatModel(supervisorModel)
    .subAgents(gptWriter, claudeWriter, productionAgent)
    .supervisorContext(workflowInstructions)
    .build();
```

### Lesson 2: Managing Shared Memory (Slides 11-15)
**Significant Update Needed**

The current approach (ChatMemory) still exists for the manual mode, but the agentic mode handles memory differently:

**NEW: Supervisor Context as Memory**
- The supervisor maintains conversation state
- Previous scenes passed explicitly to writers
- State tracked via tools (`registerScene`, `buildOpera`)

**Keep:** The concept that models need context
**Update:** Show how context flows through the supervisor

### Lesson 3: AI Comedy Through Absurdity (Slides 16-18)
**Keep:** This content is still relevant and entertaining
**Consider:** Note that both manual and agentic modes produce similar absurd content

### Lesson 4: Modern Java + AI (Slides 19-23)
**Keep:** Records, virtual threads, pattern matching, text blocks
**Update:** Add agentic-specific patterns:
```java
// Static factory on records
Opera.Scene scene = Opera.Scene.parse(sceneNumber, content, author);

// Supervisor context with text blocks
.supervisorContext("""
        You are orchestrating opera creation...
        """)
```

### NEW SECTION: Lessons from Agentic Migration

**Add slides covering:**

1. **Token Limits Break Agent Communication**
   - JSON truncation with large payloads
   - Solution: `maxTokens(8192)`

2. **Agents Role-Play Tools Instead of Invoking Them**
   - LLMs describe vs perform
   - Solution: Explicit prompt engineering

3. **LLMs Summarize by Default**
   - Full content requires explicit instruction
   - "COMPLETE content, not summaries"

4. **Manual vs Agentic Tradeoffs**
   - Table comparing the approaches
   - When to use which

5. **Debugging Multi-Agent Systems**
   - Observability challenges
   - Logging strategies

### Bonus Lessons Section (Slides 24-34)
**Keep:** Most of this content is still relevant:
- API Evolution & Resilience
- Rate Limiting (still applies to image generation)
- Test-Driven AI Development
- Voice Narration
- AI Critic

**Update:**
- Image generation now uses Gemini Nano Banana, not gpt-image-1
- Model names updated throughout

### Live Demo Section (Slides 35-37)
**Update:** Show both modes:
```bash
# Agentic mode (default)
./gradlew run

# With custom scene count
./gradlew run --args="3"

# Manual mode
./gradlew run --args="--manual"
```

### Key Takeaways (Slide 44)
**Update list to include agentic-specific lessons:**
1. **Supervisor pattern** enables autonomous orchestration
2. **Token limits** matter for agent communication
3. **Explicit instructions** prevent tool role-playing and summarization
4. **Manual vs Agentic** is a real tradeoff - offer both
5. **Modern Java** makes AI integration elegant
6. (Keep existing relevant points)

### APIs & Technologies (Slide 48)
**Update:**
- GPT-5 → GPT-5.2
- Claude Opus 4.1 → Claude Opus 4.5
- gpt-image-1 → Gemini Nano Banana (gemini-3-pro-image-preview)
- Add: langchain4j-agentic framework

---

## New Slides to Add

### Architecture Diagram
Show the supervisor pattern with Mermaid or ASCII art

### @Agent and @Tool Annotations
Show how interfaces become agents and methods become tools

### Supervisor Context Deep Dive
Show the actual supervisorContext string and explain each part

### The Three Fixes
Dedicated slide showing the three issues we solved:
| Issue | Symptom | Solution |
|-------|---------|----------|
| JSON truncation | OutputParsingException | maxTokens(8192) |
| Tool role-playing | Empty collectedScenes | "MUST INVOKE TOOL" |
| Summarization | Plot summaries in libretto | "COMPLETE content" |

---

## Content to Remove or Reduce

- Heavy emphasis on `ChatMemory` (still relevant but less central)
- Detailed gpt-image-1 content (replaced by Gemini)
- Some of the "continuation challenges" content (unless relevant)

---

## Recommended Slide Order

1. Title + Contact
2. The Premise (hook)
3. **NEW:** Two Approaches: Manual vs Agentic
4. **NEW:** The Supervisor Pattern (architecture diagram)
5. **NEW:** Building Agents with AgenticServices
6. **NEW:** The Supervisor Context
7. **NEW:** Lessons Learned (3-4 slides on the fixes)
8. **NEW:** Manual vs Agentic Tradeoffs
9. Modern Java + AI (updated)
10. The AI Orchestra (updated - all the integrations)
11. Live Demo
12. Key Takeaways (updated)
13. Resources & Thank You

---

## Timing Estimate

If original presentation was ~30-40 minutes:
- Remove/condense: ~10 minutes of ChatMemory/gpt-image-1 detail
- Add: ~15 minutes on agentic patterns and lessons learned
- Result: Similar length, shifted focus
