package com.shuvostechworld.aranyadrishti.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shuvostechworld.aranyadrishti.data.local.ReportEntity
import com.shuvostechworld.aranyadrishti.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.content.res.ColorStateList

class ReportAdapter(private val onReportClick: (Long) -> Unit) : ListAdapter<ReportEntity, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReportViewHolder(private val binding: ItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(report: ReportEntity) {
            binding.tvCategory.text = report.category
            binding.tvUrgency.text = report.urgency
            
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
            binding.tvDate.text = dateStr

            if (report.isSynced) {
                binding.ivSyncStatus.setImageResource(com.shuvostechworld.aranyadrishti.R.drawable.ic_cloud_done)
                binding.ivSyncStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#388E3C"))
                binding.ivSyncStatus.contentDescription = "Status: Report synced to cloud"
            } else {
                binding.ivSyncStatus.setImageResource(com.shuvostechworld.aranyadrishti.R.drawable.ic_pending_clock)
                binding.ivSyncStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                binding.ivSyncStatus.contentDescription = "Status: Sync pending, waiting for network"
            }

            // Screen reader description
            binding.root.contentDescription = "Report category: ${report.category}. Urgency: ${report.urgency}. Date: $dateStr. " + binding.ivSyncStatus.contentDescription
            
            binding.root.setOnClickListener {
                onReportClick(report.id)
            }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<ReportEntity>() {
        override fun areItemsTheSame(oldItem: ReportEntity, newItem: ReportEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReportEntity, newItem: ReportEntity): Boolean {
            return oldItem == newItem
        }
    }
}
