package com.sesameware.smartyard_oem.ui.main.address.cctv_video.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.R

class DetailButtonsAdapter(
    context: Context,
    private var currentPos: Int,
    private val mItems: List<CCTVData>,
    private val mCallback: listenerGeneric<Int>
) : RecyclerView.Adapter<DetailButtonsAdapter.DetailButtonsViewHolder>() {
    private var previousPos: Int = -1

    val colorWhite = ContextCompat.getColor(context, R.color.white)
    val colorBlack = ContextCompat.getColor(context, R.color.black)
    val adapter = this

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailButtonsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cctv_detail_button, parent, false)
        return DetailButtonsViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailButtonsViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun selectButton(position: Int) {
        if (currentPos == position) return

        previousPos = currentPos
        currentPos = position
        adapter.notifyItemChanged(previousPos)
        adapter.notifyItemChanged(currentPos)
//        adapter.notifyDataSetChanged()
    }

    inner class DetailButtonsViewHolder(itemView: View) : RecyclerView.ViewHolder
    (itemView) {
        private val button: TextView = itemView.findViewById(R.id.cctv_id)

        fun onBind(position: Int) {
            button.text = (position + 1).toString()
            setSelected(position == currentPos)
            button.setOnClickListener { listener(position) }
        }
        private fun listener(position: Int) {
            mCallback(position)
        }

        private fun setSelected(boolean: Boolean) {
            button.isSelected = boolean
            button.setTextColor(if (boolean) colorWhite else colorBlack)
        }
    }
}
