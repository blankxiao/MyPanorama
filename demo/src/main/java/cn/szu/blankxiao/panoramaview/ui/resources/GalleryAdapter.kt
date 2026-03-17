package cn.szu.blankxiao.panoramaview.ui.resources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskListItemDto
import cn.szu.blankxiao.panoramaview.ui.task.TaskAdapter
import coil.load

class GalleryAdapter(
    private val onClick: (PanoramaTaskListItemDto) -> Unit
) : ListAdapter<PanoramaTaskListItemDto, GalleryAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumbnail: ImageView = view.findViewById(R.id.iv_gallery_thumbnail)
        private val tvName: TextView = view.findViewById(R.id.tv_gallery_name)
        private val tvMode: TextView = view.findViewById(R.id.tv_gallery_mode)
        private val tvTime: TextView = view.findViewById(R.id.tv_gallery_time)

        fun bind(item: PanoramaTaskListItemDto) {
            tvName.text = item.name ?: "未命名"
            tvMode.text = TaskAdapter.mapMode(item.mode)
            tvTime.text = item.createdAt?.replace("T", " ")?.take(19) ?: ""

            // 使用 Coil 加载缩略图：自动处理缓存、生命周期、占位图
            val url = item.resultOssUrl
            ivThumbnail.load(url) {
                placeholder(R.drawable.ic_nav_panorama)
                error(R.drawable.ic_nav_panorama)
                crossfade(true)
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PanoramaTaskListItemDto>() {
            override fun areItemsTheSame(a: PanoramaTaskListItemDto, b: PanoramaTaskListItemDto) = a.id == b.id
            override fun areContentsTheSame(a: PanoramaTaskListItemDto, b: PanoramaTaskListItemDto) = a == b
        }
    }
}
