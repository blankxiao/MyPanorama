package cn.szu.blankxiao.panoramaview

import android.app.Application
import cn.szu.blankxiao.panorama.utils.ImageUtil

/**
 * @author BlankXiao
 * @description InitApplication
 * @date 2025-10-27 0:46
 */
class InitApplication: Application() {
	override fun onCreate() {
		super.onCreate()
		ImageUtil.init(this)
	}

}