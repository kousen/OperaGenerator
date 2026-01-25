plugins {
    id("java")
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.kousenit"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Required for Embabel parameter name inference
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
    // Embabel repositories
    maven {
        name = "embabel-releases"
        url = uri("https://repo.embabel.com/artifactory/libs-release")
        mavenContent {
            releasesOnly()
        }
    }
    maven {
        name = "embabel-snapshots"
        url = uri("https://repo.embabel.com/artifactory/libs-snapshot")
        mavenContent {
            snapshotsOnly()
        }
    }
    maven {
        name = "Spring Milestones"
        url = uri("https://repo.spring.io/milestone")
    }
}

val embabelVersion = "0.3.2"

dependencies {
    // Embabel Agent Framework
    implementation("com.embabel.agent:embabel-agent-starter:$embabelVersion")
    implementation("com.embabel.agent:embabel-agent-starter-openai:$embabelVersion")
    implementation("com.embabel.agent:embabel-agent-starter-anthropic:$embabelVersion")
    implementation("com.embabel.agent:embabel-agent-starter-shell:$embabelVersion")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")

    // For ElevenLabs API JSON serialization
    implementation("com.google.code.gson:gson:2.13.2")

    // For Gemini image generation (Nano Banana)
    implementation("com.google.genai:google-genai:1.36.0")

    // To remove security vulnerabilities - force both logback versions to match
    implementation("commons-codec:commons-codec:1.20.0")
    implementation("ch.qos.logback:logback-core:1.5.25")
    implementation("ch.qos.logback:logback-classic:1.5.25")
    implementation("org.apache.commons:commons-lang3:3.20.0")

    // For playing generated audio files
    implementation("javazoom:jlayer:1.0.1")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
