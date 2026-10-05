import groovy.json.JsonOutput
import java.util.Properties

plugins {
    base
    id("net.fabricmc.fabric-loom") version "1.17.1" apply false
}

apply(from = rootProject.file("gradle/moulconfig263.gradle"))

val catalog = Properties().apply { file("gradle/targets.properties").inputStream().use(::load) }
val targets = catalog.getProperty("targets").split(',').map(String::trim)
val modVersion = providers.gradleProperty("mod_version").get()

allprojects {
    group = "org.hypixelskyblockmods.crittersafariesp"
    version = modVersion
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.notenoughupdates.org/releases/")
    }
}

subprojects {
    if (name !in targets) return@subprojects
    val mc = catalog.getProperty("$name.minecraft")
    version = "$modVersion+mc$mc"
    apply(plugin = "net.fabricmc.fabric-loom")
    dependencies {
        add("minecraft", "com.mojang:minecraft:$mc")
        add("implementation", "net.fabricmc:fabric-loader:0.19.3")
        add("implementation", "net.fabricmc.fabric-api:fabric-api:${catalog.getProperty("$name.fabric_api")}")
        add("implementation", "net.fabricmc:fabric-language-kotlin:1.13.12+kotlin.2.4.0")
        add("implementation", if (mc == "26.3") rootProject.extra["moulConfig263"]!! else "org.notenoughupdates.moulconfig:${catalog.getProperty("$name.moul_config")}:4.7.2")
        add("include", if (mc == "26.3") project(":moulconfig263") else "org.notenoughupdates.moulconfig:${catalog.getProperty("$name.moul_config")}:4.7.2")
        add("testImplementation", platform("org.junit:junit-bom:5.11.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
    }
    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        clientOnlyMinecraftJar()
        runs { named("client") { runDir("run/$mc") } }
    }
    extensions.configure<net.fabricmc.loom.api.fabricapi.FabricApiExtension> {
        configureTests {
            createSourceSet.set(true)
            modId.set("crittersafariesp-test")
            enableGameTests.set(false)
            enableClientGameTests.set(true)
        }
    }
    extensions.configure<SourceSetContainer> {
        named("main") {
            java.setSrcDirs(listOf(rootProject.file("src/main/java"), rootProject.file("src/$mc/java")))
            resources.setSrcDirs(listOf(rootProject.file("src/main/resources")))
        }
        named("test") { java.setSrcDirs(listOf(rootProject.file("src/test/java"))) }
        named("gametest") {
            java.setSrcDirs(listOf(rootProject.file("src/gametest/java"), rootProject.file("src/$mc/gametest/java")))
            resources.setSrcDirs(listOf(rootProject.file("src/gametest/resources")))
        }
    }
    tasks.withType<JavaCompile>().configureEach { options.release.set(25); options.encoding = "UTF-8" }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    tasks.named<ProcessResources>("processResources") {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", mc)
        filesMatching("fabric.mod.json") { expand("version" to project.version, "minecraft_version" to mc) }
    }
    tasks.named<Jar>("jar") {
        from(rootProject.file("THIRD_PARTY.md"))
        archiveBaseName.set("CritterSafariUtility")
        from(rootProject.file("LICENSE")) { rename { "LICENSE_CritterSafariUtility" } }
    }
}

tasks.named("build") { dependsOn(targets.map { ":versions:$it:build" }) }
tasks.named("clean") { dependsOn(targets.map { ":versions:$it:clean" }) }
tasks.register("releaseManifest") {
    val output = layout.buildDirectory.file("release/manifest.json")
    outputs.file(output)
    inputs.file("gradle/targets.properties")
    inputs.property("mod_version", modVersion)
    doLast {
        val data = mapOf("modVersion" to modVersion, "targets" to targets.map { target ->
            val mc = catalog.getProperty("$target.minecraft")
            mapOf("project" to target, "minecraft" to mc,
                "jar" to "versions/$target/build/libs/CritterSafariUtility-$modVersion+mc$mc.jar")
        })
        output.get().asFile.apply { parentFile.mkdirs(); writeText(JsonOutput.prettyPrint(JsonOutput.toJson(data)) + "\n") }
    }
}
