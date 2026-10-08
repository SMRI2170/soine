import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "app.soine.shared"
        compileSdk = 37
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            // Treat every new kotlinc warning as an error so the CI smoke
            // gate (build.yml "commonTest + Android debug + lint") fails
            // the PR on a regression. New warnings are blocked at PR time;
            // existing warnings must be fixed and the baseline shrunk each
            // cycle. See docs/ci-quality-policy.md for the policy slice.
            allWarningsAsErrors.set(true)
        }

        withHostTest {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Mirror the Android target's allWarningsAsErrors setting for every
    // iOS target so a new warning in any source set fails the PR. The
    // tasks.withType<...>().configureEach path is the non-deprecated way
    // to set the new compilerOptions DSL across all compilation tasks.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
