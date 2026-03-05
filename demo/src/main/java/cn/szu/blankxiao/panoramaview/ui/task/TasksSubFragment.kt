package cn.szu.blankxiao.panoramaview.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.viewmodel.PanoramaViewModel
import kotlinx.coroutines.launch

class TasksSubFragment : Fragment() {

    private val vm: PanoramaViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary))
        swipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.surface))

        val rvTasks = view.findViewById<RecyclerView>(R.id.rv_tasks)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_tasks_empty)

        val adapter = TaskAdapter { task ->
            Toast.makeText(requireContext(), "任务 #${task.id}: ${TaskAdapter.mapStatus(task.status)}", Toast.LENGTH_SHORT).show()
        }
        rvTasks.adapter = adapter

        swipeRefresh.setOnRefreshListener { vm.loadTaskList() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.toastMessage.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.taskListState.collect { state ->
                    swipeRefresh.isRefreshing = state.refreshing

                    if (state.tasks.isEmpty() && !state.refreshing) {
                        rvTasks.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                        val tvEmpty = view.findViewById<TextView>(R.id.tv_tasks_empty_text)
                        if (state.errorMsg != null) {
                            tvEmpty?.text = state.errorMsg
                            tvEmpty?.setTextAppearance(R.style.Widget_PanoramaView_Text_Error)
                        } else {
                            tvEmpty?.setText(R.string.tasks_empty_hint)
                            tvEmpty?.setTextAppearance(R.style.Widget_PanoramaView_Text_Empty)
                        }
                    } else if (state.tasks.isNotEmpty()) {
                        rvTasks.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                        adapter.submitList(state.tasks)
                    }
                }
            }
        }

        if (vm.taskListState.value.tasks.isEmpty()) {
            vm.loadTaskList()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadTaskList()
    }
}
