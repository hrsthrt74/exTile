plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 将仓库中的 Markdown 法律文档复制到构建产物，保证 GitHub 与 App 使用同一份来源文件。
val copyLegalDocuments by tasks.registering(Copy::class) {
    from(rootProject.file("docs")) {
        include("PRIVACY_POLICY.md", "TERMS_OF_SERVICE.md")
    }
    into(layout.buildDirectory.dir("generated/legalAssets"))
}

android {
    namespace = "com.hrsthrt74.qstile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hrsthrt74.qstile"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        aidl = true
    }

    // 使用确定的生成目录，避免 Android SourceSet API 拒绝 Provider 类型目录。
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/legalAssets").get().asFile)
}

// Android 资源任务不会自动依赖自定义 Copy 任务，因此显式建立构建顺序。
tasks.named("preBuild") {
    dependsOn(copyLegalDocuments)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    implementation(libs.reorderable)
    implementation(libs.devicecompat)
    implementation(libs.clarity.compose)
    implementation(libs.markwon.core)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
