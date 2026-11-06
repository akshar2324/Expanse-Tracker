pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // JitPack only serves txnparse (bank SMS parsing). Restricting it to that group keeps
    // every other dependency resolving from Google and Maven Central.
    exclusiveContent {
      forRepository { maven("https://jitpack.io") }
      filter { includeModule("com.github.akshar2324", "txnparse") }
    }
  }
}

rootProject.name = "My Application"

include(":app")
