package com.sesameware.smartyard_oem.ui.main.address.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sesameware.domain.model.response.Story
import com.sesameware.smartyard_oem.databinding.ItemStoryBinding

class StoriesAdapter(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<Story, StoriesAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoryViewHolder(private val binding: ItemStoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(story: Story) {
            binding.tvStoryTitle.text = story.title
            binding.tvStorySubtitle.text = story.subtitle
            Glide.with(binding.ivStoryImage)
                .load(story.imageUrl)
                .centerCrop()
                .into(binding.ivStoryImage)
            
            binding.root.setOnClickListener { onStoryClick(story) }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem.url == newItem.url && oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Story, newItem: Story): Boolean {
            return oldItem == newItem
        }
    }
}
