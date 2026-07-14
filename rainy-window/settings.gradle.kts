@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}
dependencyResolutionManagement {
  repositories {
    // Local Cloudy build (com.github.skydoves:cloudy:0.7.2-SNAPSHOT), published via
    // `SNAPSHOT=true ./gradlew :cloudy:publishToMavenLocal` in the Cloudy checkout — first so it wins
    // over any same-version artifact a remote repo might also serve.
    mavenLocal()
    google()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

rootProject.name = "rainy-window"
include(":composeApp")
