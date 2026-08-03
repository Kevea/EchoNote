plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Fuehrt ein Kommando aus und liefert die getrimmte Ausgabe, bei jedem Fehler "".
fun cmdOutput(vararg args: String): String = try {
    val proc = ProcessBuilder(*args).directory(rootDir).start()
    val text = proc.inputStream.bufferedReader().readText().trim()
    proc.waitFor()
    if (proc.exitValue() == 0) text else ""
} catch (e: Exception) {
    ""
}

// Versionsangaben zur Build-Zeit aus Git ableiten - gleiches Vorgehen wie beim
// UpTown Dashboard, damit beide Apps dieselbe Anzeige haben.
val appVersionName = "1.14"
val gitShort = cmdOutput("git", "rev-parse", "--short", "HEAD").ifBlank { "local" }
val commitSubject = cmdOutput("git", "log", "-1", "--pretty=%s")
val mergePr = Regex("#(\\d+)").find(commitSubject)?.groupValues?.get(1)
val runNumber = System.getenv("GITHUB_RUN_NUMBER")
val buildLabel = when {
    mergePr != null -> "#$mergePr"
    !runNumber.isNullOrBlank() -> "build $runNumber"
    else -> "dev"
}
val repoUrl = "https://github.com/Kevea/Plainvoice"

android {
    namespace = "com.plainvoice.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.plainvoice.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = appVersionName

        // Lesbare Version fuer die Anzeige in den Einstellungen, z. B. "1.14 (#42)".
        buildConfigField("String", "APP_VERSION", "\"$appVersionName ($buildLabel)\"")
        buildConfigField("String", "APP_COMMIT", "\"$gitShort\"")
        buildConfigField("String", "APP_REPO_URL", "\"$repoUrl\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Signierung wird ausschliesslich ueber Umgebungsvariablen gefuettert (CI).
    // Fehlt KEYSTORE_PATH - etwa bei einem lokalen Build - laeuft assembleRelease
    // weiterhin durch und erzeugt ein unsigniertes Artefakt, statt abzubrechen.
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Ab AGP 8 muss BuildConfig ausdruecklich eingeschaltet werden.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room exportiert das Schema nach app/schemas. Die Dateien gehoeren ins Repo:
// ohne sie kann Room kuenftige Migrationen nicht gegenpruefen.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Offline PDF text extraction for note import - Android has no built-in API for this.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // DocumentFile wrapper for writing into a user-picked SAF folder (auto-export).
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
