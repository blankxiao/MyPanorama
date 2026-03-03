package cn.szu.blankxiao.panoramaview.ui.resources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.ui.task.TaskAdapter
import cn.szu.blankxiao.panoramaview.viewmodel.PanoramaViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GallerySubFragment : Fragment() {

    private val vm: PanoramaViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gallery_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val rvGallery = view.findViewById<RecyclerView>(R.id.rv_gallery)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty)
        val btnDemo = view.findViewById<MaterialButton>(R.id.btn_demo_panorama)

        val adapter = GalleryAdapter { item ->
            val intent = Intent(requireContext(), PanoramaActivity::class.java).apply {
                putExtra(PanoramaActivity.EXTRA_RESOURCE_ID, item.id)
                putExtra(PanoramaActivity.EXTRA_RESOURCE_NAME, item.name)
                putExtra(PanoramaActivity.EXTRA_RESOURCE_TIME, item.createdAt?.replace("T", " ")?.take(19))
                putExtra(PanoramaActivity.EXTRA_RESOURCE_MODE, TaskAdapter.mapMode(item.mode))
                putExtra(PanoramaActivity.EXTRA_RESOURCE_URL, item.resultOssUrl)
            }
            startActivity(intent)
        }
        rvGallery.adapter = adapter

        btnDemo.setOnClickListener {
            startActivity(Intent(requireContext(), PanoramaActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener { vm.loadResultList() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.toastMessage.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.resultListState.collect { state ->
                    swipeRefresh.isRefreshing = state.refreshing

                    if (state.results.isEmpty() && !state.refreshing) {
                        rvGallery.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                    } else if (state.results.isNotEmpty()) {
                        rvGallery.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.GONE
                        adapter.submitList(state.results)
                    }
                }
            }
        }

        if (vm.resultListState.value.results.isEmpty()) {
            vm.loadResultList()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadResultList()
    }
}
