plugins {
    java
    application
    id("com.code-intelligence.jazzer") version "0.24.0" apply false
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

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    "fuzzImplementation"("com.code-intelligence:jazzer-api:0.24.0")
}

sourceSets {
    create("fuzz") {
        compileClasspath += sourceSets["main"].output + configurations["compileClasspath"]
        runtimeClasspath += sourceSets["main"].output + configurations["runtimeClasspath"]
        java.srcDir("src/fuzz/java")
    }
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
