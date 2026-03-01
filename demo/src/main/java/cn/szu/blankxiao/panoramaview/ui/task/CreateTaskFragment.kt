package cn.szu.blankxiao.panoramaview.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.szu.blankxiao.panoramaview.R

/**
 * 创建 Tab：用于创建新的全景生成任务（待实现）。
 */
class CreateTaskFragment : Fragment() {

    override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_create, container, false)
}