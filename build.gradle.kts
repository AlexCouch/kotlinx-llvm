//val antlrVersion = "4.7.1"
val antlrKotlinVersion = "0.0.4"

buildscript {
    // we have to re-declare this here :-(
    val antlrKotlinVersion = "0.0.4"

    repositories{
        maven("https://jitpack.io")
    }

    dependencies {
        // add the plugin to the classpath
        classpath("com.strumenta.antlr-kotlin:antlr-kotlin-gradle-plugin:86a86f1968")
    }
}

repositories {
    // used for local development and while building by travis ci and jitpack.io
    mavenLocal()
    // used to download antlr4
    mavenCentral()
    // used to download korio
    jcenter()
    // used to download antlr-kotlin-runtime
    maven("https://jitpack.io")
}

plugins {
    kotlin("jvm") version "1.3.61"
    // do not add the plugin here, it contains only a task
    //id("com.strumenta.antlr-kotlin") version "0.0.5"
}

// in antlr-kotlin-plugin <0.0.5, the configuration was applied by the plugin.
// starting from verison 0.0.5, you have to apply it manually:
tasks.register<com.strumenta.antlrkotlin.gradleplugin.AntlrKotlinTask>("generateKotlinGrammarSource") {
    // the classpath used to run antlr code generation
    antlrClasspath = configurations.detachedConfiguration(
            // antlr itself
            // antlr is transitive added by antlr-kotlin-target,
            // add another dependency if you want to choose another antlr4 version (not recommended)
            // project.dependencies.create("org.antlr:antlr4:$antlrVersion"),

            // antlr target, required to create kotlin code
            project.dependencies.create("com.strumenta.antlr-kotlin:antlr-kotlin-target:86a86f1968")
    )
    maxHeapSize = "64m"
    packageName = "com.couch.toylang"
//    arguments = listOf("-no-visitor", "-no-listener")
    arguments = listOf("-visitor")
    source = project.objects
            .sourceDirectorySet("antlr", "antlr")
            .srcDir("src/main/antlr").apply {
                include("*.g4")
            }
    // outputDirectory is required, put it into the build directory
    // if you do not want to add the generated sources to version control
    outputDirectory = File("build/generated-src/antlr/main")
    // use this settings if you want to add the generated sources to version control
    // outputDirectory = File("src/main/kotlin-antlr")
}

// run generate task before build
// not required if you add the generated sources to version control
// you can call the task manually in this case to update the generated sources
tasks.getByName("compileKotlin").dependsOn("generateKotlinGrammarSource")

// you have to add the generated sources to kotlin compiler source directory list
configure<SourceSetContainer> {
    named("main") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("build/generated-src/antlr/main")
            // kotlin.srcDir("src/main/kotlin-antlr")
        }
    }
    named("test"){
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("build/generated-src/antlr/main")
        }
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))
    // add antlr-kotlin-runtime-jvm
    // otherwise, the generated sources will not compile
    compile("com.strumenta.antlr-kotlin:antlr-kotlin-runtime-jvm:86a86f1968")
    compile("org.bytedeco:llvm-platform:9.0.0-1.5.2")
//    compile("me.tomassetti:kllvm:0.0.1-SNAPSHOT")
}