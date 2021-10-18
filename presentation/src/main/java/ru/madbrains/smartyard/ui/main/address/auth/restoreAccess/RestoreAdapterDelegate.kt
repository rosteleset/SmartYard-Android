package ru.madbrains.smartyard.ui.main.address.auth.restoreAccess

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import kotlinx.android.synthetic.main.item_recovery.view.checkBox
import kotlinx.android.synthetic.main.item_recovery.view.title
import ru.madbrains.smartyard.R

/**
 * @author Nail Shakurov
 * Created on 20/03/2020.
 */
class RestoreAdapterDelegate(
    var activity: Activity,
    private val clickListener: (position: Int, id: String, name: String) -> Unit
) :
    AdapterDelegate<List<RecoveryModel>>() {

    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<RecoveryModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AddressCameraViewHolder(inflater.inflate(R.layout.item_recovery, parent, false))

    override fun onBindViewHolder(
        items: List<RecoveryModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as AddressCameraViewHolder
        val recoveryItem: RecoveryModel = items[position]
        vh.apply {
            if (recoveryItem.name.contains("@"))
                tvTitle.text = "Выслать код восстановления на почту " + recoveryItem.name
            else
                tvTitle.text = "Выслать код восстановления на телефон " + recoveryItem.name

            checkbox.isChecked = recoveryItem.check

            checkbox.setOnClickListener {

                items.forEach {
                    it.check = false
                }
                items[position].check = true
                clickListener.invoke(position, recoveryItem.id, recoveryItem.name)
            }
        }
    }

    internal class AddressCameraViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.title
        val checkbox: CheckBox = itemView.checkBox
    }
}
