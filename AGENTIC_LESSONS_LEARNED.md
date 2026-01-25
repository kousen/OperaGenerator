# Lessons Learned: Multi-Agent Orchestration in Java

These lessons emerged from porting the Opera Generator to use the langchain4j-agentic framework's supervisor pattern.

---

## 1. Token Limits Break Agent Communication Silently

**The Problem:** The supervisor passes arguments to sub-agents as JSON. When scene content exceeded the default 1024 token limit, the JSON was truncated mid-response, causing `OutputParsingException`.

**The Symptom:**
```
dev.langchain4j.exception.OutputParsingException:
  Failed to parse JSON: Unexpected end-of-input
```

**The Solution:** Size your model's `maxTokens` for your largest expected payload, not just typical responses.

```java
public static final ChatModel CLAUDE_OPUS_4_5_LARGE = AnthropicChatModel.builder()
        .apiKey(ApiKeys.ANTHROPIC_API_KEY)
        .modelName("claude-opus-4-5-20251101")
        .maxTokens(8192)  // Required for passing full scene content in JSON
        .build();
```

**The Lesson:** In agentic systems, the supervisor's response contains structured data (agent selection + arguments). Large payloads need room in the token budget.

---

## 2. Agents Will "Role-Play" Tools Instead of Invoking Them

**The Problem:** The ProductionAgent would respond "Scene 1 registered successfully!" without actually calling `registerScene()`. The LLM was *describing* tool use rather than *performing* it.

**The Symptom:** `collectedScenes` list remained empty despite "success" messages in logs.

**The Solution:** Prompts must explicitly demand tool invocation:

```java
@UserMessage("""
        You are a Production Assistant. You MUST use the available tools
        to execute tasks - never just acknowledge or confirm without
        actually invoking a tool.

        CRITICAL: When asked to perform any task, you MUST call the
        appropriate tool function. Do NOT simply respond with "done"
        or "registered" - INVOKE THE ACTUAL TOOL.

        IMPORTANT: You MUST invoke the tool function. Your response
        should show the tool being called.

        Request: {{request}}
        """)
```

**The Lesson:** LLMs are trained on text that describes actions. Without explicit instruction, they'll describe tool calls rather than make them.

---

## 3. LLMs Summarize by Default - Full Content Requires Explicit Instruction

**The Problem:** The supervisor passed scene summaries to `registerScene` instead of full dialogue. The libretto ended up with plot descriptions, not actual opera text.

**The Symptom:** Generated libretto contained "Sandra expresses her feelings" instead of actual lyrics.

**The Solution:** Explicitly demand complete content in the supervisor context:

```java
.supervisorContext("""
        WORKFLOW (follow this exact sequence):
        1. Write Scene 1 with gptSceneWriter
        2. Call productionAgent with: "Register scene 1 with the
            following COMPLETE content: [paste ENTIRE scene here].
            Author: GPT-5.2"

        CRITICAL REQUIREMENTS:
        - You MUST pass the COMPLETE scene text to registerScene, not a summary
        - Include ALL dialogue, stage directions, and character lines
        - The full scene content is required for the libretto
        """)
```

**The Lesson:** LLMs compress information by default (it's useful for most tasks). When you need verbatim data passed between agents, say so explicitly and repeatedly.

---

## 4. Scene Count is a Constraint, Not an Agent Decision

**The Question:** Should we let the agent autonomously decide how many scenes to write?

**The Problem:** Writers need to know `totalScenes` to pace the story properly. Without knowing when the finale is, every scene feels like a middle scene. The final scene needs explicit instructions:

```java
String sceneInstructions = isFinalScene
    ? "This is the FINAL scene. Tie off major plot threads, resolve the romantic arc, and deliver a decisive ending."
    : "Set the stage for upcoming scenes so the opera can naturally progress to its finale.";
```

**The Decision:** Scene count remains a user parameter, not an agent decision.

**The Lesson:** Some parameters are user requirements that constrain the problem space; others are decisions the agent can make autonomously. Story structure constraints (like total length) belong to the user.

---

## 5. The Supervisor Context is Your Control Plane

**The Reality:** The `supervisorContext` string is where you encode workflow logic, agent selection rules, and sequencing requirements. It's essentially a detailed prompt that shapes all autonomous decisions.

**What Goes in Supervisor Context:**
- Which agent does what task
- The required sequence of operations
- Data requirements (complete content vs summaries)
- Termination conditions
- Error handling guidance

```java
.supervisorContext("""
        You are orchestrating opera creation. Your sub-agents are:

        1. gptSceneWriter - Use for odd-numbered scenes (1, 3, 5...)
        2. claudeSceneWriter - Use for even-numbered scenes (2, 4, 6...)
        3. productionAgent - Handles registration, building, production

        WORKFLOW (follow this exact sequence):
        1. Write Scene 1 with gptSceneWriter
        2. Call productionAgent to register it
        ...
        """)
```

**The Lesson:** Invest heavily in your supervisor context. It's the most important piece of configuration in your agentic system.

---

## 6. Manual vs Agentic is a Real Tradeoff

| Manual Orchestration | Agentic Orchestration |
|---------------------|----------------------|
| Predictable, debuggable | Autonomous, adaptive |
| You control the sequence | LLM plans the sequence |
| Rigid workflow | Flexible workflow |
| Easy to test and reproduce | Hard to reproduce exactly |
| Fast iteration | Slower (more LLM calls) |

**Our Approach:** We kept both modes in `AgenticOperaGenerator`:

```java
// Manual mode - code controls everything
public Opera generateOpera(String title, int numberOfScenes) { ... }
public void runProductionWorkflow() { ... }

// Agentic mode - supervisor decides
public String generateOperaAutonomously(int numberOfScenes) { ... }
public String generateOperaAutonomously(String request) { ... }
```

**The Lesson:** Consider offering both modes. Manual for debugging, testing, and demos where you need predictability. Agentic for production flexibility and natural language interfaces.

---

## 7. Debugging Multi-Agent Systems is Hard

**The Challenges:**
- You're inferring LLM behavior from logs
- HTTP request/response logging helps but overwhelms context
- You often can't reproduce issues exactly (LLM non-determinism)
- State is distributed across multiple agent interactions

**What Helped:**
- Structured logging at agent invocation points
- Tool call logging (what was called with what arguments)
- Intermediate state inspection (`collectedScenes`, `currentOpera`)
- Reducing log verbosity once the pattern was understood

```java
// Initially helpful for debugging
.logRequests(true)
.logResponses(true)

// Later removed to reduce noise
// (Logging disabled on CLAUDE_OPUS_4_5_LARGE)
```

**The Lesson:** Build in observability from the start. Log agent invocations, tool calls, and state transitions. Consider checkpointing intermediate results.

---

## 8. Domain Objects Should Own Their Parsing

**The Problem:** We had duplicate `parseScene()` methods in both `AgenticOperaGenerator` and `OperaTools`. Same logic, same regex, same bug potential.

**The Solution:** Move parsing to the domain object as a static factory:

```java
public record Scene(int number, String title, String author, String content) {

    private static final Pattern SCENE_PATTERN = Pattern.compile(
            "Scene\\s+(\\d+):\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);

    public static Scene parse(int expectedNumber, String content, String author) {
        Matcher matcher = SCENE_PATTERN.matcher(content);
        if (matcher.find()) {
            // ... parsing logic
            return new Scene(number, title, author, sceneContent);
        }
        return new Scene(expectedNumber, "Untitled Scene " + expectedNumber, author, content);
    }
}
```

**The Lesson:** Don't let agent infrastructure scatter domain logic across multiple classes. Keep your domain model clean and centralized.

---

## 9. Sub-Agent Design Pattern

**The Pattern:** Interfaces with `@Agent` annotation define capabilities. Tools attach to agents, not supervisors.

```java
// Interface defines the agent's capability
@Agent(description = "Writes opera scenes with dramatic dialogue")
public interface SceneWriterAgent {
    @UserMessage("""
            You are an expert opera librettist...
            Write Scene {{sceneNumber}} of {{totalScenes}}.
            """)
    String writeScene(String title, String premise, String previousScenes,
                      int sceneNumber, int totalScenes, String sceneInstructions);
}

// Build the agent with a specific model
this.gptWriter = AgenticServices
        .agentBuilder(SceneWriterAgent.class)
        .chatModel(AiModels.GPT_5_2)
        .name("gptSceneWriter")
        .description("Writes opera scenes using GPT-5.2")
        .build();

// Tools attach to the agent that needs them
this.productionAgent = AgenticServices
        .agentBuilder(ProductionAgent.class)
        .chatModel(supervisorModel)
        .tools(operaTools)  // ← Tools go here
        .build();
```

**The Lesson:** Think of sub-agents as capability contracts. The supervisor orchestrates and delegates; sub-agents execute their specialized tasks.

---

## 10. Experimental Means Experimental

**The Reality:** langchain4j-agentic is still evolving. We encountered:
- Behaviors that required prompt engineering workarounds (role-playing tools)
- Token limit issues that weren't immediately obvious
- The need for explicit, sometimes redundant instructions

**What We Did:**
- Budgeted extra time for debugging framework quirks
- Kept both manual and agentic modes so we could fall back
- Documented the workarounds for future reference

**The Lesson:** When using experimental frameworks, budget time for debugging framework quirks, not just your application logic. Expect to contribute back issues and perhaps fixes.

---

## Summary: The Agentic Mindset

Moving from manual orchestration to agentic orchestration requires a shift in thinking:

| Manual Mindset | Agentic Mindset |
|----------------|-----------------|
| "I'll call this function next" | "The supervisor will decide what's next" |
| "I know the exact sequence" | "I've described the goal and constraints" |
| "Debug by stepping through code" | "Debug by analyzing agent decisions" |
| "Errors are deterministic" | "Behavior may vary between runs" |
| "State is in my variables" | "State is distributed across agents" |

The power of agentic systems comes from their flexibility and ability to handle natural language requests. The cost is predictability and debuggability. Choose wisely based on your use case.
