import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  listOf(
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  @Suppress("OPT_IN_USAGE")
  applyDefaultHierarchyTemplate {
    common {
      group("skiko") {
        withJvm()
        withIos()
      }
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
      implementation(libs.compose.ui.backhandler)
      implementation(libs.cloudy)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
    getByName("desktopMain").dependencies {
      implementation(compose.desktop.currentOs)
    }
  }
}

android {
  namespace = "io.github.l2hyunwoo.compose.morphsearch"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "io.github.l2hyunwoo.compose.morphsearch"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

compose.desktop {
  application {
    mainClass = "io.github.l2hyunwoo.compose.morphsearch.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "MorphingSearchTextField"
      packageVersion = "1.0.0"
    }
  }
}
