plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.routinercompense"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.routinercompense"
        minSdk = 26
        targetSdk = 36
        versionCode = 9500
        versionName = "9.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

base {
    archivesName.set("Routine_Recompenses")
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register("copyToGoogleDrive") {
    doLast {
        val driveFolder = file("G:/Mon Drive/Routine_App")
        if (driveFolder.exists()) {
            val rootFolder = rootProject.rootDir
            val apkSrc = file("${rootFolder}/Routine_Recompenses.apk")
            val jsonSrc = file("${rootFolder}/version.json")
            if (apkSrc.exists()) {
                apkSrc.copyTo(file("${driveFolder}/Routine_Recompenses.apk"), overwrite = true)
            }
            if (jsonSrc.exists()) {
                jsonSrc.copyTo(file("${driveFolder}/version.json"), overwrite = true)
            }
        }
    }
}