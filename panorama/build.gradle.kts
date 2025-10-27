import org.gradle.kotlin.dsl.implementation

plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.kotlin.android)
}

android {
	namespace = "cn.szu.blankxiao.panorama"
	compileSdk = 36

	defaultConfig {
		minSdk = 24

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		consumerProguardFiles("consumer-rules.pro")
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


	implementation("com.facebook.fresco:fresco:2.6.0")
	implementation("com.facebook.fresco:animated-gif:2.6.0")
	implementation("com.facebook.fresco:animated-webp:2.6.0")
	implementation("com.facebook.fresco:webpsupport:2.6.0")
	implementation("com.facebook.fresco:imagepipeline-okhttp3:2.6.0")

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}