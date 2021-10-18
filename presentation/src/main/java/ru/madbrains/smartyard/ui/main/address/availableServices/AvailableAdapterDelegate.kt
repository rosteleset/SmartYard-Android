package ru.madbrains.smartyard.ui.main.address.availableServices

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import kotlinx.android.synthetic.main.item_available.view.checkBox
import kotlinx.android.synthetic.main.item_available.view.description
import kotlinx.android.synthetic.main.item_available.view.title
import ru.madbrains.smartyard.R

/**
 * @author Nail Shakurov
 * Created on 2020-02-13.
 */
class AvailableAdapterDelegate(private var clickCheckBox: () -> Unit) :
    AdapterDelegate<List<AvailableModel>>() {

    override fun isForViewType(items: List<AvailableModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_available, parent, false)
        return AddressCameraViewHolder(view)
    }

    override fun onBindViewHolder(
        items: List<AvailableModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as AddressCameraViewHolder
        val parentsModel: AvailableModel = items[position]
        vh.apply {
            tvTitle.text = parentsModel.title
            tvDescription.text = parentsModel.description
            checkbox.isChecked = parentsModel.check
            checkbox.isActivated = true
            if (!parentsModel.active) {
                checkbox.isEnabled = false
                tvDescription.isEnabled = false
                tvTitle.isEnabled = false
            }
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                items[position].check = isChecked
            }
            checkbox.setOnClickListener {
                clickCheckBox.invoke()
            }
        }
    }

    internal class AddressCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.title
        val tvDescription: TextView = itemView.description
        val checkbox: CheckBox = itemView.checkBox
    }
}
