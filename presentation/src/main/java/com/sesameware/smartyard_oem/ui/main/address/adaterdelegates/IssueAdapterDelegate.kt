package com.sesameware.smartyard_oem.ui.main.address.adaterdelegates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.ui.main.address.adapters.ParentListAdapterSetting
import com.sesameware.smartyard_oem.ui.main.address.models.interfaces.DisplayableItem
import com.sesameware.smartyard_oem.ui.main.address.models.IssueModel

/**
 * @author Nail Shakurov
 * Created on 24/04/2020.
 */
class IssueAdapterDelegate(private val setting: ParentListAdapterSetting) : AdapterDelegate<List<DisplayableItem>>() {

    override fun isForViewType(items: List<DisplayableItem>, position: Int): Boolean =
        items[position] is IssueModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_issue, parent, false)
        return IssueViewHolder(view)
    }

    override fun onBindViewHolder(
        items: List<DisplayableItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as IssueViewHolder
        val itemIssueModel: IssueModel = items[position] as IssueModel
        vh.apply {
            tvAddress.text = itemIssueModel.address
            ivQrCode.setOnClickListener {
                setting.clickQrCode.invoke()
            }
            this.itemView.setOnClickListener {
                setting.clickItemIssue.invoke(itemIssueModel)
            }
        }
    }

    internal class IssueViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var tvAddress: TextView = itemView.findViewById<View>(R.id.tvAddress) as TextView
        var ivQrCode: ImageView = itemView.findViewById<View>(R.id.ivQrCode) as ImageView
    }
}
