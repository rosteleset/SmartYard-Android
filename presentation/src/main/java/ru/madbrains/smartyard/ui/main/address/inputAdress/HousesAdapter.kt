package ru.madbrains.smartyard.ui.main.address.inputAdress

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import ru.madbrains.domain.model.response.HousesData

/**
 * @author Nail Shakurov
 * Created on 18/03/2020.
 */

class HousesAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val list: MutableList<HousesData>
) : ArrayAdapter<HousesData>(context, layoutResource, list), Filterable {

    private var listLocationData: MutableList<HousesData> = list

    fun addData(list: List<HousesData>) {
        listLocationData.clear()
        listLocationData.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun getCount(): Int = listLocationData.size

    override fun getItem(position: Int): HousesData? = listLocationData[position]

    override fun getItemId(position: Int): Long = listLocationData[position].houseId.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(
            layoutResource,
            parent,
            false
        ) as TextView
        view.text = "${listLocationData[position].number}"
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                listLocationData = filterResults.values as MutableList<HousesData>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val queryString = charSequence?.toString()?.toLowerCase()

                val filterResults = FilterResults()
                filterResults.values = if (queryString == null || queryString.isEmpty())
                    list
                else
                    list.filter {
                        it.number.toLowerCase().contains(queryString)
                    }

                return filterResults
            }
        }
    }
}
