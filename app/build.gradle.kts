import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
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
    // lite: aplikasi terpisah yang hanya membaca mushaf per halaman, tanpa internet (lihat docs/FORK_LITE.md). Dibuat di
    // dimensi yang sama agar nama tugas varian lain (assembleGithubDebug dan seterusnya) tidak berubah.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "true")
            buildConfigField("boolean", "LITE", "false")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
            buildConfigField("boolean", "LITE", "false")
        }
        create("lite") {
            dimension = "distribution"
            applicationIdSuffix = ".lite"
            buildConfigField("boolean", "UPDATER_ENABLED", "false")
            buildConfigField("boolean", "LITE", "true")
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
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Android Lint di CI: temuan lama dicatat di lint-baseline.xml, temuan baru menggagalkan build.
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkDependencies = false
        // local.properties hanya ada di mesin pengembang (tidak di git) dan ditulis oleh Android Studio.
        disable += "PropertyEscape"
    }

    // Robolectric (tes JVM dengan Android tiruan) butuh resource aplikasi.
    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    // Memasang Baseline Profile saat aplikasi dipasang dari luar Play Store (APK GitHub).
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

// Varian pengukuran dari plugin Baseline Profile (benchmarkRelease, nonMinifiedRelease) memakai ID terpisah, supaya
// pengujian otomatis di perangkat (yang memasang lalu MENCOPOT aplikasi target) tidak menyentuh aplikasi terpasang.
// Harus lewat API varian: plugin membuat build type itu sendiri, jadi applicationIdSuffix di blok buildTypes tidak berlaku.
// Profil hasil generator digabung ke src/main agar varian github dan play sama-sama memakainya.
baselineProfile {
    mergeIntoMain = true
}

// Varian lite memakai quran.db ramping (tanpa data penjelajahan, info surah, transliterasi) yang dibangun dari
// quran.db penuh oleh tools/build_lite_db.py saat build. Berkas hasilnya menimpa src/main/assets/quran.db di APK lite.
abstract class LiteDbTask : DefaultTask() {
    @get:InputFile abstract val source: RegularFileProperty
    @get:InputFile abstract val script: RegularFileProperty
    @get:Input abstract val python: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty
    @get:Inject abstract val exec: ExecOperations

    @TaskAction
    fun build() {
        exec.exec {
            commandLine(python.get(), script.get().asFile.path, source.get().asFile.path, outputDir.get().file("quran.db").asFile.path)
        }
    }
}

val liteDb = tasks.register<LiteDbTask>("buildLiteDb") {
    source.set(layout.projectDirectory.file("src/main/assets/quran.db"))
    script.set(rootProject.layout.projectDirectory.file("tools/build_lite_db.py"))
    // Di Windows biasanya "python": ./gradlew -Ppython=python ...
    python.set(providers.gradleProperty("python").orElse("python3"))
}

androidComponents {
    onVariants { variant ->
        if (variant.flavorName == "lite") variant.sources.assets?.addGeneratedSourceDirectory(liteDb, LiteDbTask::outputDir)
        val type = variant.buildType.orEmpty()
        if (type.startsWith("benchmark") || type.startsWith("nonMinified")) {
            variant.applicationId.set("io.zakkyhidayat.quran.bench")
        }
    }
}
