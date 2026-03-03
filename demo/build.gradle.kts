plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
}

android {
	namespace = "cn.szu.blankxiao.panoramaview"
	compileSdk = 36

	defaultConfig {
		applicationId = "cn.szu.blankxiao.panoramaview"
		minSdk = 24
		targetSdk = 36
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
	implementation(libs.kotlinx.coroutines.android)
	implementation(libs.androidx.lifecycle.viewmodel.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.viewpager2)
	implementation(libs.androidx.swiperefreshlayout)

	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.android)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}