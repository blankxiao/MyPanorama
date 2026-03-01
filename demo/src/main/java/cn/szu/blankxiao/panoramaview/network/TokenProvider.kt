package cn.szu.blankxiao.panoramaview.network

/**
 * 提供当前登录 token，用于请求头。未登录时返回 null。
 * 后续可改为从 DataStore 读取。
 */
fun interface TokenProvider {
    fun getToken(): String?
}
