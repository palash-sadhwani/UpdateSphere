import java.io.FileInputStream
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun localProperty(name: String): String =
    localProperties.getProperty(name, "")

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
}

android {
    namespace = "com.example.chat"
    compileSdk = 33
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.chat"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            buildConfigString(localProperty("cloudinary.cloud_name"))
        )
        buildConfigField(
            "String",
            "CLOUDINARY_API_KEY",
            buildConfigString(localProperty("cloudinary.api_key"))
        )
        buildConfigField(
            "String",
            "CLOUDINARY_API_SECRET",
            buildConfigString(localProperty("cloudinary.api_secret"))
        )
        manifestPlaceholders["MAPS_API_KEY"] = localProperty("maps.api_key")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    // Core Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")




    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Circular ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Add these if you're using ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // If you see errors about Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    implementation("com.google.android.gms:play-services-base:18.3.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-bom:32.7.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.cloudinary:cloudinary-android:2.3.1")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")

    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:android-maps-utils:2.4.0")


    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation ("org.osmdroid:osmdroid-mapsforge:6.1.16")// For offline maps

    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("org.osmdroid:osmdroid-android:6.1.11")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.google.android.gms:play-services-location:20.0.0")
    implementation("com.google.firebase:firebase-firestore:24.4.1")
    implementation("com.google.firebase:firebase-auth:21.1.0")
}
