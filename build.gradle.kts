plugins {
    kotlin("jvm") version "1.3.70"
}

allprojects{
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
        maven("https://maven.pkg.github.com/Strumenta/kolasu")

    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.0.2")
    compile("com.strumenta.antlr-kotlin:antlr-kotlin-runtime-jvm:86a86f1968")
    compile("org.bytedeco:llvm-platform:9.0.0-1.5.2")
}

tasks{
    test{
        useJUnitPlatform()
    }
}