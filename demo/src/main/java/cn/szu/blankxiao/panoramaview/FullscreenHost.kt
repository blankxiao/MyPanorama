package cn.szu.blankxiao.panoramaview

/**
 * 供全景页全屏时通知 Activity 隐藏/显示底部导航栏，实现无导航栏的完全全屏。
 */
interface FullscreenHost {
    fun setFullscreen(fullscreen: Boolean)
}
