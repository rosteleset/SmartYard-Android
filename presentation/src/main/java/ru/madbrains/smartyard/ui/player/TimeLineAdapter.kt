package ru.madbrains.smartyard.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateTimeFormat
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.minuteFormat
import timber.log.Timber

class TimeLineAdapter(
    private val mExoPlayerViewModel: ExoPlayerViewModel,
    private val context: Context
) : RecyclerView.Adapter<TimeLineAdapter.ViewHolder>() {

    private var items: TimeLineItem? = mExoPlayerViewModel.getTimeLineItem().value
    private val MAX_LINES = 10

//    inner class LineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val lineTextView: TextView = itemView.findViewById(R.id.lineTextView)
//        fun drawLine(textView: TextView, height: Int, paddingTop: Float) {
//            textView.layoutParams = ConstraintLayout.LayoutParams(
//                ConstraintLayout.LayoutParams.MATCH_PARENT,
//                height
//            ).apply {
////                topMargin = paddingTop
//            }
//            textView.y = paddingTop
//            textView.elevation = 6f
//            textView.setTextColor(Color.WHITE)
//            textView.setBackgroundColor(Color.RED)
//        }
//    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeStamp: TextView = itemView.findViewById(R.id.tv_stamp_time)
        val flexBox: FlexboxLayout = itemView.findViewById(R.id.flexboxLayout)
        val clLines: ConstraintLayout = itemView.findViewById(R.id.cl_lines)
        val lines: List<TextView> = createLines()
        private fun createLines(): List<TextView> {
            val lines = mutableListOf<TextView>()
            for (i in 0 until MAX_LINES) {
                val line = TextView(context)
                line.visibility = View.GONE
                clLines.addView(line)
                lines.add(line)
            }
            return lines
        }

        fun drawLine(textView: TextView, height: Int, paddingTop: Float) {
            textView.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                height
            ).apply {
//                topMargin = paddingTop
            }
            textView.y = paddingTop
            textView.elevation = 6f
            textView.setTextColor(Color.WHITE)
            textView.setBackgroundColor(Color.RED)
        }

        fun updateLine(textView: TextView?, height: Int, paddingTop: Int) {
            textView?.layoutParams?.height = height
            textView?.y = paddingTop.toFloat()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: TimeLineItem, scale: Int) {
        mExoPlayerViewModel.setScale(scale)
//        mExoPlayerViewModel.SCALE = scale
        items = newData
        notifyDataSetChanged()
    }

    private fun prepareDrawLine(holder: ViewHolder, position: Int, heightInOneSecond: Float) {
        val rangeTimeItems = items!!.rangeTime[position].entries.first()

        rangeTimeItems.value.forEachIndexed { index, item ->
            if (item != null && index < holder.lines.size) {
                val itemTimeFirst = item.second * 1000
                val itemTimeSecond = item.first * 1000
                val noDvrSeconds = (itemTimeSecond - itemTimeFirst) / 1000
                val secondsToStartPoint = (rangeTimeItems.key - itemTimeFirst) / 1000
                val paddingTop = secondsToStartPoint * heightInOneSecond
                val strokeWidth = heightInOneSecond * noDvrSeconds

                holder.drawLine(holder.lines[index], strokeWidth.toInt(), paddingTop)
                holder.lines[index].visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.d("TimeLineAdapter onCreateViewHolder")
        items = mExoPlayerViewModel.getTimeLineItem().value
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.time_line_flex, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items?.rangeTime?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Скрываем все линии перед отрисовкой новых
        holder.lines.forEach { it.visibility = View.GONE }

        val rangeTimeItems = items!!.rangeTime[position].entries.first()
        val scale = mExoPlayerViewModel.scale.value!!
        val heightInOneSecond = (holder.flexBox.height.toFloat() / scale) / 60

        for (i in 0 until holder.flexBox.childCount - 1) {
            if (rangeTimeItems.value.isNotEmpty()) {
                val gapFirst = rangeTimeItems.value[0]?.first!!
                val gapSecond = rangeTimeItems.value[0]?.second!!
                val itemTimeFirst = gapFirst * 1000
                val itemTimeSecond = gapSecond * 1000

                val secondsToStartPoint = (rangeTimeItems.key - itemTimeFirst) / 1000
                val secondsToEndPoint = (rangeTimeItems.key - itemTimeSecond) / 1000

                val heightInFlexBox =
                    holder.flexBox[0].y + holder.flexBox[holder.flexBox.childCount - 2].y
                val timeInHeightOneLine = (heightInFlexBox / scale)
                val pointToNoDvr1 = secondsToEndPoint / timeInHeightOneLine
                val pointToNoDvr2 = secondsToStartPoint / timeInHeightOneLine

                Timber.d(
                    "TIMESECOMNDSAAS heightInFlexBox $heightInFlexBox | Y=${holder.flexBox[i].y} | y1 $pointToNoDvr1 ${
                        dateTimeFormat.format(
                            itemTimeFirst
                        )
                    } y2 $pointToNoDvr2 ${dateTimeFormat.format(itemTimeSecond)}"
                )

                if (holder.flexBox[i].y >= pointToNoDvr1 && holder.flexBox[i - 1].y <= pointToNoDvr2) {
                    prepareDrawLine(holder, position, heightInOneSecond)
                }
            }
        }

        val timeCurrent = minuteFormat.format(items!!.rangeTime[position].entries.first().key)
//        mExoPlayerViewModel.setChosenDateIndex(position)
        val layoutParams = holder.flexBox.layoutParams
        if (mExoPlayerViewModel.scaleHeight.value != null) {
            layoutParams.height = mExoPlayerViewModel.scaleHeight.value!!
            holder.flexBox.layoutParams = layoutParams
        }
        holder.timeStamp.text = timeCurrent
    }
}
//TODO Линия отсутсвия архива рисуется не верно со смещеннием +1
