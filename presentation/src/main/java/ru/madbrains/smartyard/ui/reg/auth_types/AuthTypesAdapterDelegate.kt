package ru.madbrains.smartyard.ui.reg.auth_types

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import ru.madbrains.smartyard.R

class AuthTypesAdapterDelegate(
    var activity: Activity,
    private val clickListener: (methodId: String) -> Unit
) : AdapterDelegate<List<AuthTypesModel>>() {
    private var checkedPosition = -1
    private val inflater: LayoutInflater = activity.layoutInflater

    override fun isForViewType(items: List<AuthTypesModel>, position: Int): Boolean {
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return AuthTypeItemVH(inflater.inflate(R.layout.item_auth_type, parent, false))
    }

    override fun onBindViewHolder(
        items: List<AuthTypesModel>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: MutableList<Any>
    ) {
        val vh = holder as AuthTypeItemVH
        val authItem = items[position]
        vh.apply {
            tvTitle.text = authItem.name
            checkbox.isClickable = false
            checkbox.isChecked = authItem.isChecked
            holder.itemView.setOnClickListener {
                if (checkedPosition >= 0 && checkedPosition != position) {
                    items[checkedPosition].isChecked = false
                    bindingAdapter?.notifyItemChanged(checkedPosition)
                }
            }
            checkbox.isChecked = true
            checkedPosition = position
            clickListener.invoke(authItem.methodId)
        }
    }

    internal class AuthTypeItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.title)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
}
