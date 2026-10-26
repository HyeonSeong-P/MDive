// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register<Exec>("generateDesignSystem") {
    group = "design system"
    description = "Generate Kotlin tokens, catalog data, CSS and HTML from the shared design sources."
    workingDir(rootDir)
    commandLine("python3", "tools/generate_design_system.py")
}

val verifyDesignSystem by tasks.registering(Exec::class) {
    group = "verification"
    description = "Fail when checked-in design system outputs differ from their shared sources."
    workingDir(rootDir)
    commandLine("python3", "tools/generate_design_system.py", "--check")
}

val verifyLauncherIcons by tasks.registering(Exec::class) {
    group = "verification"
    description = "Verify launcher resources against the approved mark and palette."
    workingDir(rootDir)
    commandLine("python3", "tools/generate_launcher_icons.py", "--check")
}

subprojects {
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(verifyDesignSystem)
        dependsOn(verifyLauncherIcons)
    }
}
