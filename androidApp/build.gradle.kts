plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "app.soine"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.soine"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
}
