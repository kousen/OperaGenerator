plugins {
    id("java")
    id("application")
}

group = "com.kousenit"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    val overrideMain = project.findProperty("mainClass") as String?
    mainClass.set(overrideMain ?: "com.kousenit.OperaGeneratorApp")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("dev.langchain4j:langchain4j-bom:1.10.0"))
    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-core")
    implementation("dev.langchain4j:langchain4j-open-ai")
    implementation("dev.langchain4j:langchain4j-mistral-ai")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini")
    implementation("dev.langchain4j:langchain4j-ollama")
    implementation("dev.langchain4j:langchain4j-anthropic")

    implementation("ch.qos.logback:logback-classic:1.5.25")
    implementation("info.picocli:picocli:4.7.7")
    
    // For ElevenLabs API JSON serialization
    implementation("com.google.code.gson:gson:2.13.2")

    // For Gemini image generation (Nano Banana)
    implementation("com.google.genai:google-genai:1.36.0")
    implementation("commons-codec:commons-codec:1.20.0")

    // For playing generated audio files
    implementation("javazoom:jlayer:1.0.1")

    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.test {
    useJUnitPlatform {
        // Support for JUnit 5 tags
        if (project.hasProperty("test.tags")) {
            includeTags(project.property("test.tags").toString())
        }
        // Exclude expensive tests by default unless explicitly included
        if (!project.hasProperty("test.tags")) {
            excludeTags("expensive", "integration")
        }
    }
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

// Task to run only unit tests (no API calls)
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        excludeTags("integration", "expensive")
    }
    description = "Run only unit tests (no API calls)"
}

// Task to run integration tests (API calls but not expensive)
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
        excludeTags("expensive")
    }
    description = "Run integration tests (API calls)"
}

// Task to run expensive tests (full opera generation)
tasks.register<Test>("expensiveTest") {
    useJUnitPlatform {
        includeTags("expensive")
    }
    description = "Run expensive tests (full opera generation)"
}

// ── Claude Code Teams pipeline steps ──────────────────────────────────
// Each step can be run independently by a team agent.

fun createPipelineTask(name: String, description: String) =
    tasks.register<JavaExec>(name) {
        this.description = description
        group = "pipeline"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.kousenit.PipelineSteps")
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        // Pass Gradle project properties as program arguments
        args = when (name) {
            "generateScenes" -> listOfNotNull(
                project.findProperty("operaTitle") as String? ?: "",
                project.findProperty("sceneCount") as String? ?: "5"
            )
            else -> listOfNotNull(
                project.findProperty("operaJson") as String? ?: ""
            )
        }

        // Route to the correct step method via system property
        systemProperty("pipeline.step", name)
    }

createPipelineTask("generateScenes", "Generate opera scenes with alternating AI models")
createPipelineTask("generateImages", "Generate illustrations with Gemini Nano Banana")
createPipelineTask("generateNarration", "Generate audio narration with ElevenLabs")
createPipelineTask("generateCritique", "Generate critical review with Gemini 3 Pro")
