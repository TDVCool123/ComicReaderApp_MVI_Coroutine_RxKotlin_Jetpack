// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories { mavenCentral() }
}

subprojects {
  apply(from = "${rootProject.rootDir}/spotless.gradle.kts")
}

allprojects {
  apply(plugin = "com.github.ben-manes.versions")

  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-reflect") {
        useVersion(versions.kotlin.core)
      }
    }
  }

  repositories {
    google()
    jcenter()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.repsy.io/mvn/chrynan/public")
    maven {
      url = uri("http://dl.bintray.com/amulyakhare/maven")
      isAllowInsecureProtocol = true
    }
  }
  dependencies {
        // Aquí se debe usar la notación adecuada para Kotlin DSL
        implementation("com.miguelcatalan:materialsearchview:1.4.0")
    }
}


tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }

