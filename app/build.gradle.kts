// 导入 java.util.Properties 用于读取签名配置文件
// （在 Kotlin DSL 里 "java" 会被解析成 java 插件扩展，必须用 import 才能引用 java.util 包）
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // 构建期收集所有依赖的许可信息，生成 aboutlibraries.json 供许可页读取
    alias(libs.plugins.aboutlibraries.plugin)
}

// 将仓库中的 Markdown 法律文档复制到构建产物，保证 GitHub 与 App 使用同一份来源文件。
val copyLegalDocuments by tasks.registering(Copy::class) {
    from(rootProject.file("docs")) {
        include("PRIVACY_POLICY.md", "TERMS_OF_SERVICE.md")
    }
    into(layout.buildDirectory.dir("generated/legalAssets"))
}

// 读取项目根目录下的 keystore.properties（含签名密码等敏感信息，已被 .gitignore 忽略）
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.hrsthrt74.qstile"
    compileSdk = 37

    // release 签名配置：仅当 keystore.properties 存在且填写完整时才生效，
    // 缺失时保持默认（unsigned），避免其他人克隆仓库后构建失败
    signingConfigs {
        if (
            keystoreProperties["storeFile"] != null &&
            keystoreProperties["storePassword"] != null &&
            keystoreProperties["keyAlias"] != null &&
            keystoreProperties["keyPassword"] != null
        ) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "com.hrsthrt74.qstile"
        minSdk = 34
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // 挂载上面的签名配置，使 assembleRelease 直接产出已签名的 APK
            signingConfig = signingConfigs.findByName("release")
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
    // SplashScreen 兼容库：安装启动画面并支持 keepOnScreenCondition 挂屏控制
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.lifecycle.viewmodel.compose)
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
    // AboutLibraries 核心库：解析插件生成的许可数据
    implementation(libs.aboutlibraries.core)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
