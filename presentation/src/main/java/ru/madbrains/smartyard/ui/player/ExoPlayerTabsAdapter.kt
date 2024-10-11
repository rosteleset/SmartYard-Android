package ru.madbrains.smartyard.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.madbrains.domain.utils.listenerGeneric
import ru.madbrains.smartyard.R
import timber.log.Timber



class ExoPlayerTabsAdapter(
    private val items: List<CCTVDataItem>,
    private val mCallback: listenerGeneric<Int>
) : RecyclerView.Adapter<ExoPlayerTabsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tv_title_camera)
        val imagePreview: ImageView = itemView.findViewById(R.id.iv_preview_camera)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_adapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.camera_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position].name
        try {
            Glide.with(holder.itemView)
                .load(items[position].previewUrl)
                .into(holder.imagePreview)
            holder.imagePreview.visibility = View.VISIBLE
        } catch (e: Exception) {
            Timber.d("ExoPlayerTabsAdapter onBindViewHolder ${e.message}")
        }

        holder.imagePreview.setOnClickListener {
            mCallback(position)
        }
    }

}