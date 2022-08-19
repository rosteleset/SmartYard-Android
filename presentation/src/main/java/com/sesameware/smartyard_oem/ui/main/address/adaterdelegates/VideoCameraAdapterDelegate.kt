package com.sesameware.smartyard_oem.ui.main.address.adaterdelegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.adapters.ParentListAdapterSetting
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.VideoCameraModel

/**
 * @author Nail Shakurov
 * Created on 2020-02-11.
 */
class VideoCameraAdapterDelegate(private val setting: ParentListAdapterSetting) :
    AdapterDelegate<List<DisplayableItem>>() {

    private val inflater: LayoutInflater = LayoutInflater.from(setting.context)

    override fun isForViewType(items: List<DisplayableItem>, position: Int): Boolean =
        items[position] is VideoCameraModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        VideoCameraViewHolder(inflater.inflate(R.layout.item_video_camera, parent, false))

    override fun onBindViewHolder(
        items: List<DisplayableItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as VideoCameraViewHolder
        val videoCamera: VideoCameraModel = items[position] as VideoCameraModel
        vh.caption.text = videoCamera.caption
        vh.count.text = videoCamera.counter.toString()
        vh.itemView.setOnClickListener { setting.clickCamera(videoCamera) }
    }

    internal class VideoCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var caption: TextView = itemView.findViewById<View>(R.id.tvTitle) as TextView
        var count: TextView = itemView.findViewById<View>(R.id.tvCount) as TextView
    }
}
