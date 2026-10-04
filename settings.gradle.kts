import java.util.Properties

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "CritterSafariUtility"
val catalog = Properties().apply { file("gradle/targets.properties").inputStream().use(::load) }
catalog.getProperty("targets").split(',').map(String::trim).forEach { include("versions:$it") }
