package ru.madbrains.smartyard.ui.main.address.adaterdelegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import net.cachapa.expandablelayout.ExpandableLayout
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.adapters.ChildListAdapter
import ru.madbrains.smartyard.ui.main.address.adapters.ParentListAdapterSetting
import ru.madbrains.smartyard.ui.main.address.models.ParentModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem

/**
 * @author Nail Shakurov
 * Created on 2020-02-12.
 */
class AddressAdapterDelegate(private val setting: ParentListAdapterSetting) :
    AdapterDelegate<List<DisplayableItem>>() {

    private val inflater: LayoutInflater = LayoutInflater.from(setting.context)

    override fun isForViewType(items: List<DisplayableItem>, position: Int): Boolean =
        items[position] is ParentModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddressCameraViewHolder(inflater.inflate(R.layout.item_parent_recycler, parent, false))

    override fun onBindViewHolder(
        items: List<DisplayableItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        holder as AddressCameraViewHolder
        val parentsModel: ParentModel = items[position] as ParentModel
        holder.textView.text = parentsModel.addressTitle
        holder.recyclerView.apply {
            layoutManager =
                LinearLayoutManager(holder.recyclerView.context, RecyclerView.VERTICAL, false)
        }
        if (parentsModel.isExpanded) {
            holder.coll.expand(false)
            holder.imageView.setImageResource(R.drawable.ic_arrow_top)
        } else {
            holder.coll.collapse(false)
            holder.imageView.setImageResource(R.drawable.ic_arrow_bottom)
        }
        holder.itemView.setOnClickListener {
            if (holder.coll.isExpanded) {
                holder.coll.collapse()
                holder.imageView.setImageResource(R.drawable.ic_arrow_bottom)
                setting.clickPos.invoke(position, false)
            } else {
                holder.coll.expand()
                holder.coll.setOnExpansionUpdateListener { expansionFraction, _ ->
                    if (expansionFraction == 1F) {
                        setting.clickPos.invoke(position, true)
                    }
                }
                holder.imageView.setImageResource(R.drawable.ic_arrow_top)
            }
        }

        val adapter = ChildListAdapter(setting)
        adapter.items = parentsModel.children
        holder.recyclerView.adapter = adapter
    }

    internal class AddressCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val recyclerView: RecyclerView = itemView.findViewById(R.id.rv_child)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val coll: ExpandableLayout = itemView.findViewById(R.id.expandable_layout)
        val imageView: ImageView = itemView.findViewById(R.id.imageView6)
    }
}
