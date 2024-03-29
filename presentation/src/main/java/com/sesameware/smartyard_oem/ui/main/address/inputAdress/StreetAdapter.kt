package com.sesameware.smartyard_oem.ui.main.address.inputAdress

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.sesameware.domain.model.response.StreetsData
import java.util.*

/**
 * @author Nail Shakurov
 * Created on 12/03/2020.
 */
class StreetAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val list: MutableList<StreetsData>
) : ArrayAdapter<StreetsData>(context, layoutResource, list), Filterable {
    private var listStreetsData: MutableList<StreetsData> = list

    fun addData(list: List<StreetsData>) {
        listStreetsData.clear()
        listStreetsData.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun getCount(): Int = listStreetsData.size

    override fun getItem(position: Int): StreetsData? = listStreetsData[position]

    override fun getItemId(position: Int): Long = listStreetsData[position].streetId.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(
            layoutResource,
            parent,
            false
        ) as TextView
        view.text = "${listStreetsData[position].name}"
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                listStreetsData = filterResults.values as MutableList<StreetsData>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val queryString = charSequence?.toString()?.lowercase(Locale.getDefault())
                val filterResults = FilterResults()
                filterResults.values = if (queryString == null || queryString.isEmpty())
                    list
                else
                    list.filter {
                        it.name.lowercase(Locale.getDefault()).contains(queryString)
                    }
                return filterResults
            }
        }
    }
}
