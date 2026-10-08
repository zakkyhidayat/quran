plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Versi dari git tag vX.Y.Z; versionCode = X*10000 + Y*100 + Z.
val gitTag: String = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.getOrElse("")

val semver: Triple<Int, Int, Int> = Regex("""^v(\d+)\.(\d+)\.(\d+)""").find(gitTag)
    ?.destructured
    ?.let { (major, minor, patch) -> Triple(major.toInt(), minor.toInt(), patch.toInt()) }
    ?: Triple(0, 1, 0)

// Penandatanganan rilis dari variabel lingkungan (GitHub Actions) bila tersedia; tanpa itu rilis tidak ditandatangani.
val releaseKeystore: String? = System.getenv("RELEASE_KEYSTORE_PATH")

android {
    namespace = "io.zakkyhidayat.quran"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.zakkyhidayat.quran"
        minSdk = 26
        targetSdk = 37
        versionCode = semver.first * 10000 + semver.second * 100 + semver.third
        versionName = "${semver.first}.${semver.second}.${semver.third}"
        // Katalog paket terjemahan unduhan; paket diambil relatif terhadap alamat ini (lihat docs/DATA_SOURCES.md).
        buildConfigField("String", "GITHUB_REPO", "\"zakkyhidayat/quran\"")
        buildConfigField("String", "TRANSLATION_CATALOG_URL", "\"https://github.com/zakkyhidayat/quran/releases/download/translations/catalog.json\"")
    }

    // github: APK di GitHub Releases dengan pembaruan dari dalam aplikasi. play: tanpa pembaruan sendiri (kebijakan Play).
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Seperti rilis (R8, tanpa debuggable) tetapi ditandatangani kunci debug dan ber-ID terpisah, untuk mengukur
        // kelancaran di perangkat tanpa mengganggu aplikasi terpasang: ./gradlew :app:installGithubBenchmark
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".bench"
            matchingFallbacks += "release"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
