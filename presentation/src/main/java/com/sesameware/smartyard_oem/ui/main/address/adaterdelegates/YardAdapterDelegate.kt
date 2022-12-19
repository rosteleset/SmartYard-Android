package com.sesameware.smartyard_oem.ui.main.address.adaterdelegates

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.adapters.ParentListAdapterSetting
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.Yard

/**
 * @author Nail Shakurov
 * Created on 2020-02-11.
 */
class YardAdapterDelegate(
    private val setting: ParentListAdapterSetting
) : AdapterDelegate<List<DisplayableItem>>() {

    private val inflater: LayoutInflater = LayoutInflater.from(setting.context)

    override fun isForViewType(items: List<DisplayableItem>, position: Int): Boolean =
        items[position] is Yard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        VideoCameraViewHolder(inflater.inflate(R.layout.item_yard, parent, false))

    override fun onBindViewHolder(
        items: List<DisplayableItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as VideoCameraViewHolder
        val yard: Yard = items[position] as Yard
        vh.apply {
            caption.text = yard.caption
            image.setImageResource(yard.image)
            tbOpen.isChecked = false
            tbOpen.setOnClickListener {
                setting.clickOpen.invoke(yard.domophoneId, yard.doorId)
                tbOpen.isClickable = false
                val handler = Handler()
                handler.postDelayed(
                    {
                        tbOpen.isChecked = false
                        tbOpen.isClickable = true
                    },
                    3000
                )
            }
        }
    }

    internal class VideoCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var caption: TextView = itemView.findViewById<View>(R.id.textView4) as TextView
        var image: ImageView = itemView.findViewById<View>(R.id.ivImage) as ImageView
        var tbOpen: ToggleButton = itemView.findViewById<View>(R.id.tbOpen) as ToggleButton
    }
}
