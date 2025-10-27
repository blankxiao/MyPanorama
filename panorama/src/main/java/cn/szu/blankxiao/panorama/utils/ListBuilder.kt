package cn.szu.blankxiao.panorama.utils

/**
 * @author BlankXiao
 * @description ListBuilder
 * @date 2025-10-26 21:18
 */
class ListBuilder<T> {
	val list = ArrayList<T>()

	fun add(vararg items: T){
		for (item in items){
			list.add(item)
		}
	}

}