import java.util.Properties

// local.properties 파일에서 속성 값을 불러오는 함수
fun getLocalProperty(propertyName: String, defaultValue: String = ""): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    return properties.getProperty(propertyName, defaultValue)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.choongang.frombirth_app"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.choongang.frombirth_app"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties 값 사용
        buildConfigField("String", "KAKAO_API_KEY", "\"${getLocalProperty("KAKAO_API_KEY")}\"")
        buildConfigField("String", "FRONTEND_URL", "\"${getLocalProperty("FRONTEND_URL")}\"")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.kakao.sdk:v2-user:2.20.6")
    implementation("com.google.code.gson:gson")
    implementation ("androidx.security:security-crypto:1.1.0-alpha03")
    implementation("com.google.android.gms:play-services-location:21.0.1")
}