plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	id("com.google.gms.google-services")
	id("com.google.firebase.crashlytics")
}

android {
	namespace = "cn.szu.blankxiao.panoramaview"
	compileSdk = 36

	buildFeatures {
		buildConfig = true
	}

	defaultConfig {
		// 精简语言
		resConfigs("zh", "en")
        // 精简 ABI：主流手机都是 arm64
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
		applicationId = "cn.szu.blankxiao.panoramaview"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		create("release") {
			storeFile = file("../mypanorama.keystore")
			storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "123456"
			keyAlias = System.getenv("KEY_ALIAS") ?: "release"
			keyPassword = System.getenv("KEY_PASSWORD") ?: "123456"
		}
	}

	buildTypes {
		release {
			// 代码压缩
			isMinifyEnabled = true
			// 资源压缩
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("release")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.fragment.ktx)
	implementation(libs.androidx.navigation.fragment.ktx)
	implementation(libs.androidx.navigation.ui.ktx)
	implementation(project(":panorama"))

	// 后端接入：HTTP + JSON + WebSocket(OkHttp) + 协程 + Token 存储
	implementation(libs.retrofit)
	implementation(libs.retrofit.converter.moshi)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging)
	implementation(libs.moshi)
	implementation(libs.moshi.kotlin)
	implementation(libs.kotlin.reflect) // Moshi 反射必需
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.androidx.lifecycle.viewmodel.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.viewpager2)
	implementation(libs.androidx.swiperefreshlayout)

	// Firebase / Crashlytics
	implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
	implementation("com.google.firebase:firebase-crashlytics")
	implementation("com.google.firebase:firebase-analytics")

	// 卡顿检测：JankStats（每帧回调，可打日志/写报告）
	implementation(libs.androidx.metrics.performance)

	// 图片加载：Coil
	implementation(libs.coil)

	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.android)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	// 内存泄漏检测：仅 debug 构建，自动安装，无需代码
	debugImplementation(libs.leakcanary.android)
}