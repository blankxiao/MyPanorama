package cn.szu.blankxiao.panoramaview.ui.resources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import cn.szu.blankxiao.panoramaview.R
import com.google.android.material.button.MaterialButton

/**
 * 资源 Tab 内的"展示"子页：列表展示已完成任务的全景图，点击进入全景图展示。
 */
class GallerySubFragment : Fragment() {

    override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gallery_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvGallery = view.findViewById<RecyclerView>(R.id.rv_gallery)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty)
        val btnDemo = view.findViewById<MaterialButton>(R.id.btn_demo_panorama)

        // TODO: 接入后端数据后替换为真实列表
        rvGallery.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE

        btnDemo.setOnClickListener {
            startActivity(Intent(requireContext(), PanoramaActivity::class.java))
        }
    }
}
