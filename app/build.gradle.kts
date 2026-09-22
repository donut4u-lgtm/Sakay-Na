
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sakyna.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sakyna.app"
        minSdk = 23
        targetSdk = 35

        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("SAKAYNA_KEYSTORE_PATH")
            val keystorePassword = System.getenv("SAKAYNA_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("SAKAYNA_KEY_ALIAS")

            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")

    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
