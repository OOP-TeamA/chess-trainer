plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.flatlaf)
    implementation(libs.gson)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "com.oopteama.chess.Main"
    applicationDefaultJvmArgs = listOf(
        "-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua",
        "-Dapple.awt.application.name=ChessMate"
    )
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    archiveFileName.set("chess-trainer.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    val runtimeClasspath = configurations.runtimeClasspath
    from({
        runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
