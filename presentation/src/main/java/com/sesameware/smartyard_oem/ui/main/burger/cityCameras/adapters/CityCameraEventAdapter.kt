package com.sesameware.smartyard_oem.ui.main.burger.cityCameras.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.response.CCTVYoutubeData
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.R
import kotlin.math.min

class CityCameraEventAdapter(
    private val eventItems: List<CCTVYoutubeData>,
    private var currentSize: Int,
    private val eventCallback: listenerGeneric<Int>
) : RecyclerView.Adapter<CityCameraEventAdapter.CityCameraEventViewHolder>() {

    val adapter = this

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityCameraEventAdapter.CityCameraEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city_camera_event, parent, false)
        return CityCameraEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityCameraEventAdapter.CityCameraEventViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        return min(eventItems.size, currentSize)
    }

    inner class CityCameraEventViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvEvent: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val clEvent: ConstraintLayout = itemView.findViewById(R.id.clEventItem)

        fun onBind(position: Int) {
            tvEvent.text = eventItems[position].title
            clEvent.setOnClickListener {
                eventCallback(position)
            }
        }
    }

    fun currentSize(): Int {
        return if (currentSize < eventItems.size) {
            currentSize
        } else {
            eventItems.size
        }
    }

    fun setCurrentSize(newSize: Int) {
        currentSize = min(eventItems.size, newSize)
        adapter.notifyDataSetChanged()
    }
}