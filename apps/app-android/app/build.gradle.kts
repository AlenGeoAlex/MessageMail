plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "me.alenalex.messageforwarder"
    compileSdk = 35

    val verCode: Int = (findProperty("versionCode") as String?)?.toInt() ?: 1
    val verName: String = (findProperty("versionName") as String?) ?: "1.0"

    defaultConfig {
        applicationId = "me.alenalex.messageforwarder"
        minSdk = 24
        targetSdk = 35
        versionCode = verCode
        versionName = verName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksFile = file(findProperty("keystoreFile") as String? ?: "dummy.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = findProperty("keystorePassword") as String?
                keyAlias = findProperty("keyAlias") as String?
                keyPassword = findProperty("keyPassword") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation (libs.autoupdater)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.work.runtime)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}