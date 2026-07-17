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
    google()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

rootProject.name = "morphing-search-textfield"
include(":composeApp")
