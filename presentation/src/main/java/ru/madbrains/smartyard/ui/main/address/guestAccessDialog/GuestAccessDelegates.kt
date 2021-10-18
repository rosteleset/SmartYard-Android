package ru.madbrains.smartyard.ui.main.address.guestAccessDialog

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import ru.madbrains.smartyard.R

/**
 * @author Nail Shakurov
 * Created on 2020-02-19.
 */
class GuestAccessDelegates(activity: Activity, var onDelete: (position: Int) -> Unit) : AdapterDelegate<List<GuestAccessModel>>() {

    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<GuestAccessModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        VideoCameraViewHolder(inflater.inflate(R.layout.item_guest_access, parent, false))

    override fun onBindViewHolder(
        items: List<GuestAccessModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as VideoCameraViewHolder
        val guestAccessModel: GuestAccessModel = items[position]
        vh.tvNumber.text = guestAccessModel.name
        vh.ivDelete.setOnClickListener {
            onDelete.invoke(position)
        }
    }

    internal class VideoCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var tvNumber: TextView = itemView.findViewById<View>(R.id.tvNumber) as TextView
        var ivDelete: ImageView = itemView.findViewById<View>(R.id.ivDelete) as ImageView
    }
}
