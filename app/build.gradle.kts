plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ovh.gabrielhuav.sensores_escom_v2"
    compileSdk = 35

    defaultConfig {
        applicationId = "ovh.gabrielhuav.sensores_escom_v2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.okhttp)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.0.1") // La versión puede variar
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.17")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.cardview:cardview:1.0.0")
}