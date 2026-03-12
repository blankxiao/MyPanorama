package cn.szu.blankxiao.panoramaview

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import cn.szu.blankxiao.panorama.utils.ImageUtil
import cn.szu.blankxiao.panoramaview.data.TokenManager
import cn.szu.blankxiao.panoramaview.network.WebSocketManager
import com.google.firebase.BuildConfig

class InitApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase 初始化
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")

        ImageUtil.init(this)
        TokenManager.getInstance(this)
        WebSocketManager.init(this)
    }
}
