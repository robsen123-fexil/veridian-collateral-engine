plugins {
    java
    application
}

group = "com.veridian.collateral"
version = "1.4.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    create("fuzz") {
        compileClasspath += sourceSets["main"].output + configurations["compileClasspath"]
        runtimeClasspath += sourceSets["main"].output + configurations["runtimeClasspath"]
        java.srcDir("src/fuzz/java")
    }
}

val jazzerStandalone: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    add("fuzzImplementation", "com.code-intelligence:jazzer-api:0.24.0")
    jazzerStandalone("com.code-intelligence:jazzer-standalone:0.24.0")
}

tasks.register<Copy>("stageJazzer") {
    from(jazzerStandalone)
    into(layout.buildDirectory.dir("jazzer"))
    rename { "jazzer-standalone.jar" }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.veridian.collateral.tools.VeridianCtl")
}

tasks.register<JavaExec>("runCtl") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.veridian.collateral.tools.VeridianCtl")
}

listOf("BatchFuzzer", "RouterFuzzer", "SessionFuzzer", "FixFuzzer", "SwiftFuzzer").forEach { name ->
    tasks.register<JavaExec>("run$name") {
        group = "fuzz"
        dependsOn("compileJava", "compileFuzzJava")
        classpath = sourceSets["fuzz"].runtimeClasspath
        mainClass.set("com.veridian.collateral.fuzz.$name")
        jvmArgs("-Xmx512m")
    }
}

tasks.register<Jar>("fuzzJar") {
    archiveBaseName.set("veridian-fuzz")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets["main"].output)
    from(sourceSets["fuzz"].output)
    dependsOn("compileJava", "compileFuzzJava")
}

tasks.named("stageJazzer") {
    dependsOn("compileJava", "compileFuzzJava")
}
