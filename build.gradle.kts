plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.serezk4"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.projectlombok:lombok:1.18.36")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("org.apache.logging.log4j:log4j-api:2.24.2")
    implementation("org.apache.logging.log4j:log4j-core:2.24.2")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.2")
    implementation("com.puppycrawl.tools:checkstyle:10.20.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Include compiled classes and resources
    from(sourceSets.main.get().output)

    // Include runtime dependencies, excluding signature files
    from(configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { it.name },
            "Main-Class" to "com.serezk4.core.Main"
        )
    }
}

// Use ShadowJar for creating the fat JAR
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("") // Removes "-all" from the output JAR name

    manifest {
        attributes(
            "Main-Class" to "com.serezk4.core.Main"
        )
    }
}

// Make `shadowJar` the default JAR task
tasks.build {
    dependsOn(tasks.shadowJar)
}