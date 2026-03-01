package cn.szu.blankxiao.panoramaview.ui.resouces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.szu.blankxiao.panoramaview.ui.resouces.GallerySubFragment
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.ui.task.TasksSubFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 资源 Tab：顶部 TabLayout 切换"展示"和"创建"两个子页，支持左右滑动。
 */
class ResourcesFragment : Fragment() {

    private val tabTitles by lazy {
        arrayOf(getString(R.string.tab_gallery), getString(R.string.tab_create))
    }

    override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_resources, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> GallerySubFragment()
                else -> TasksSubFragment()
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
			tab.text = tabTitles[position]
		}.attach()
    }
}