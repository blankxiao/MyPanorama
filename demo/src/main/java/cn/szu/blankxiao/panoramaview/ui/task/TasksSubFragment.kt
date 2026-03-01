package cn.szu.blankxiao.panoramaview.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cn.szu.blankxiao.panoramaview.R

/**
 * 资源 Tab 内的"创建"子页：列表展示各任务的进度，点击进入任务详情。
 */
class TasksSubFragment : Fragment() {

    override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTasks = view.findViewById<RecyclerView>(R.id.rv_tasks)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_tasks_empty)

        // TODO: 接入后端数据后替换为真实列表
        rvTasks.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }
}