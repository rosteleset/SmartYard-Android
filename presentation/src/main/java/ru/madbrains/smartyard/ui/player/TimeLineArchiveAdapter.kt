package ru.madbrains.smartyard.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.madbrains.domain.utils.listenerGenericA
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.databinding.ItemLineBinding
import ru.madbrains.smartyard.databinding.TimeLineFlexBinding
import timber.log.Timber
import java.lang.NullPointerException
import java.nio.charset.Charset

sealed class  Res {
    class Ok(val data: Int): Res()
}

class TimeLineArchiveAdapter(
    private val context: Context,
    private val mCallback: listenerGenericA<Int?, Int?>
) : RecyclerView.Adapter<ViewHolder>() {
    private var items: TimeLineItem? = null
    private val linesId: MutableList<Pair<Int,List<Int>>> = mutableListOf()
    var SCALE = 15
    var scaleHeight = 330
    class TimeLineViewHolder(item: View) : ViewHolder(item) {
        val binding = TimeLineFlexBinding.bind(item)
        val flexBox = binding.flexboxLayout
        val timeStamp = binding.tvStampTime
        val itemLineLayout = binding.clLines
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.time_line_flex, parent, false)
        return TimeLineViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: TimeLineItem, scale: Int) {
        SCALE = scale
        items = newData
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is TimeLineViewHolder) {
            val timeCurrent = items?.let {
                ExoPlayerViewModel.minuteFormat.format(it.rangeTime[position].entries.first().key)
            }
            val layoutParams = holder.flexBox.layoutParams
            layoutParams.height = scaleHeight
            holder.flexBox.layoutParams = layoutParams
            holder.timeStamp.text = timeCurrent

            addLineNoDvrPeriod(position, holder)
        }
    }

    private fun addLineNoDvrPeriod(position: Int, holder: TimeLineViewHolder) {
        val noDvrList = prepareDrawLine(position)
        if (noDvrList.isNotEmpty()) {
            try {
                if (linesId.isEmpty()){
                    throw NullPointerException("")
                }else{
                    //TODO Проверка для исключения дублей, нужен рефактор
                    holder.itemLineLayout.getViewById("${position}0".toInt()).id
                }
            } catch (_: NullPointerException) {
                createLine(holder, noDvrList, position)
            }
        } else {
            //Удаление лишних линий который рецайклер пытается переиспользовать
            linesId.forEach {
                val pos = it.first
                if (position != pos){
                    val indexes = it.second
                    indexes.forEach {index ->
                        val view = holder.itemLineLayout.getViewById("$pos$index".toInt())
                        holder.itemLineLayout.removeView(view)
                    }
                }
            }
        }
    }

    private fun createLine(
        holder: TimeLineViewHolder,
        noDvrList: List<Pair<Float, Float>>,
        position: Int
    ) {
        val listIds = mutableListOf<Int>()
        noDvrList.forEachIndexed {index, pair ->
            val context = holder.itemLineLayout.context
            val y = pair.first
            val height = pair.second.toInt()

            val line = View(context)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
                )
            line.layoutParams = params
            line.setBackgroundColor(Color.GREEN)
            line.y = y
            line.id = "$position$index".toInt()
            holder.itemLineLayout.addView(line)
            listIds.add(index)
        }
        linesId.add(position to listIds)
    }

    private fun prepareDrawLine(
        position: Int,
    ): List<Pair<Float, Float>> {
        val listPair: MutableList<Pair<Float, Float>> = mutableListOf()
        val rangeTimeItems = items?.let {
            it.rangeTime[position].entries.first()
        }
        if (rangeTimeItems != null && rangeTimeItems.value.isNotEmpty()) {
            val heightInOneSecond = (scaleHeight.toFloat() / SCALE) / 60
            val timeCell = rangeTimeItems.key / 1000
            rangeTimeItems.value.forEach {
                if (it != null){
                    val gapFirst = it.first
                    val gapSecond = it.second
                    val seconds = timeCell - gapFirst
                    val y = seconds * heightInOneSecond
                    val heightPoint = (gapFirst - gapSecond) * heightInOneSecond
                    val pair = y to heightPoint
                    listPair.add(pair)
                }
            }
        }
        return listPair
    }


    override fun getItemCount() = items?.rangeTime?.size ?: 0


    companion object {
        private const val TIME_LINE = 0
        private const val LINES = 1
    }
}