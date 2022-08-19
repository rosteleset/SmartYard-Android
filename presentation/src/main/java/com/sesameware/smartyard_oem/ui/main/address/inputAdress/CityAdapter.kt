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
import com.sesameware.domain.model.response.LocationData

/**
 * @author Nail Shakurov
 * Created on 12/03/2020.
 */
class CityAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val list: MutableList<LocationData>
) : ArrayAdapter<LocationData>(context, layoutResource, list), Filterable {

    private var listLocationData: MutableList<LocationData> = list

    fun addData(list: List<LocationData>) {
        listLocationData.clear()
        listLocationData.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun getCount(): Int = listLocationData.size

    override fun getItem(position: Int): LocationData? = listLocationData[position]

    override fun getItemId(position: Int): Long = listLocationData[position].locationId.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(
            layoutResource,
            parent,
            false
        ) as TextView
        view.text = "${listLocationData[position].name}"
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                listLocationData = filterResults.values as MutableList<LocationData>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val queryString = charSequence?.toString()?.toLowerCase()

                val filterResults = FilterResults()
                filterResults.values = if (queryString == null || queryString.isEmpty())
                    list
                else
                    list.filter {
                        it.name.toLowerCase().contains(queryString)
                    }

                return filterResults
            }
        }
    }
}
