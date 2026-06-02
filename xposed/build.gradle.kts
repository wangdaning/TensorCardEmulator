plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ru.extreames.tensorcardemulator.xposed"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.extreames.tensorcardemulator.xposed"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(libs.libxposed)
}
