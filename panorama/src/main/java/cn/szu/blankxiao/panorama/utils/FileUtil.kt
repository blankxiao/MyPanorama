package cn.szu.blankxiao.panorama.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author BlankXiao
 * @description FileUtil
 * @date 2025-10-26 21:42
 */
class FileUtil {

	companion object {
		fun readFileFromRawResource(context: Context, resourceId: Int): String {
			val inputStream = context.resources.openRawResource(resourceId)
			val inputStreamReader = InputStreamReader(inputStream)
			val bufferedReader = BufferedReader(inputStreamReader)

			val body = StringBuilder()
			while (true) {
				val nextLine = bufferedReader.readLine()
				if (nextLine == null) break
				body.append(nextLine)
				body.append('\n')
			}

			return body.toString()
		}
	}

}