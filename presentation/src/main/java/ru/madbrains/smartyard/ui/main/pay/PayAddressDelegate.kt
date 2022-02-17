package ru.madbrains.smartyard.ui.main.pay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import ru.madbrains.smartyard.R

/**
 * @author Nail Shakurov
 * Created on 19.05.2020.
 */
class PayAddressDelegate(
    private val clickPos: (position: Int, payAddressModel: PayAddressModel) -> Unit
) : AdapterDelegate<List<PayAddressModel>>() {

    override fun isForViewType(items: List<PayAddressModel>, position: Int): Boolean =
        true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_pay_address, parent, false)
        return PayAddressViewHolder(view)
    }

    override fun onBindViewHolder(
        items: List<PayAddressModel>,
        position: Int,
        _holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val holder = _holder as PayAddressViewHolder
        val payAddressModel: PayAddressModel = items[position]

        payAddressModel.run {
            holder.tvAddress.text = address
            holder.itemView.setOnClickListener {
                clickPos.invoke(position, payAddressModel)
            }
        }
    }

    internal class PayAddressViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddressPayItem)
    }
}
