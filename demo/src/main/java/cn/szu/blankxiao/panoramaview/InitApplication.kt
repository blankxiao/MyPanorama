package cn.szu.blankxiao.panoramaview

import android.app.Application
import cn.szu.blankxiao.panorama.utils.ImageUtil
import cn.szu.blankxiao.panoramaview.data.TokenManager

class InitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ImageUtil.init(this)
        TokenManager.getInstance(this)
    }
}
