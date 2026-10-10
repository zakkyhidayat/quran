plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

// Membuat Baseline Profile aplikasi dengan menjalankan alur penting (buka, geser halaman, ganti mode, pengaturan) di
// perangkat yang terhubung: ./gradlew :app:generateGithubReleaseBaselineProfile
android {
    namespace = "io.zakkyhidayat.quran.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    flavorDimensions += "distribution"
    productFlavors {
        create("github") { dimension = "distribution" }
        create("play") { dimension = "distribution" }
        // Hanya agar varian lite di :app menemukan pasangannya; profil tetap dihasilkan dari varian github.
        create("lite") { dimension = "distribution" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}
