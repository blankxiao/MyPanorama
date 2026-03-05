package cn.szu.blankxiao.panoramaview.ui.task

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskListItemDto

class TaskAdapter(
    private val onClick: (PanoramaTaskListItemDto) -> Unit
) : ListAdapter<PanoramaTaskListItemDto, TaskAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val strip: View = view.findViewById(R.id.task_status_strip)
        private val tvName: TextView = view.findViewById(R.id.tv_task_name)
        private val tvStatus: TextView = view.findViewById(R.id.tv_task_status)
        private val tvMode: TextView = view.findViewById(R.id.tv_task_mode)
        private val tvTime: TextView = view.findViewById(R.id.tv_task_time)

        fun bind(item: PanoramaTaskListItemDto) {
            val ctx = itemView.context
            val statusColor = statusColor(ctx, item.status)
            strip.setBackgroundColor(statusColor)
            ViewCompat.setBackgroundTintList(tvStatus, ColorStateList.valueOf(statusColor))
            tvName.text = item.name ?: "未命名任务"
            tvStatus.text = mapStatus(item.status)
            tvMode.text = mapMode(item.mode)
            tvTime.text = item.createdAt?.replace("T", " ")?.take(19) ?: ""
            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PanoramaTaskListItemDto>() {
            override fun areItemsTheSame(a: PanoramaTaskListItemDto, b: PanoramaTaskListItemDto) = a.id == b.id
            override fun areContentsTheSame(a: PanoramaTaskListItemDto, b: PanoramaTaskListItemDto) = a == b
        }

        fun mapStatus(status: String?): String = when (status) {
            "pending" -> "等待中"
            "processing" -> "生成中"
            "completed" -> "已完成"
            "failed" -> "失败"
            else -> status ?: "未知"
        }

        fun mapMode(mode: String?): String = when (mode) {
            "text2pano" -> "文生图"
            "outpaint" -> "图文外扩"
            else -> mode ?: ""
        }
    }

    private fun statusColor(ctx: android.content.Context, status: String?): Int = when (status) {
        "pending" -> ContextCompat.getColor(ctx, R.color.status_pending)
        "processing" -> ContextCompat.getColor(ctx, R.color.status_processing)
        "completed" -> ContextCompat.getColor(ctx, R.color.status_completed)
        "failed" -> ContextCompat.getColor(ctx, R.color.status_failed)
        else -> ContextCompat.getColor(ctx, R.color.status_pending)
    }
}
