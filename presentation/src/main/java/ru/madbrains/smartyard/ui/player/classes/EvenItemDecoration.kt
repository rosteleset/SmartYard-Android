import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel
import timber.log.Timber
import java.util.Date


class EvenItemDecoration(
    context: Context,
    private val mExoPlayerViewModel: ExoPlayerViewModel
) : RecyclerView.ItemDecoration() {
    private val linePaint = Paint().apply {
        color = Color.YELLOW
        alpha = 128
    }


    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val childCount = parent.childCount
//        for (i in 0 until childCount) {
//            val child = parent.getChildAt(i)
//            // Рисование линии под каждым элементом списка
//            drawLine(c, child)
//        }

        val firstVisibleItem =
            (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val lastVisibleItemPosition =
            (parent.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        val firstVisibleView =
            parent.layoutManager?.findViewByPosition(firstVisibleItem)

        val timeFirst =
            mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![firstVisibleItem].entries.first().key
        val timeLast =
            mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![lastVisibleItemPosition].entries.first().key

        val viewFlex = firstVisibleView?.findViewById<FlexboxLayout>(R.id.flexboxLayout)
        val heightInOneSecond = (viewFlex!!.height.toFloat() / mExoPlayerViewModel.scale.value!!) / 60



        mExoPlayerViewModel.getTimeLineItem().value?.rangeTime?.forEach {
            if (it.entries.first().value.isNotEmpty()){
                Timber.d("GetTimeLineItem ${it.entries.first()} COUNT ${it.entries.first().value.size}")

            }
        }

        for (i in firstVisibleItem..lastVisibleItemPosition) {
            val timeFirsts =
                mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![i].entries.first().key
            mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![i][timeFirsts]?.forEach { item ->
                if (item != null) {
                    Timber.d("ASDSFGHHFDS $i")
                    val view = parent.layoutManager?.findViewByPosition(i)
//                    Timber.d("ITEWMGETLINET ${ExoPlayerViewModel.Companion.dateTimeFormat.format(mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![firstVisibleItem +i][timeFirsts]?.first()?.first!!.toLong() * 1000)}")
//                val itemTimeFirst = item.first * 1000
                    val itemTimeFirst = item.second * 1000
//                val itemTimeSecond = item.second * 1000
                    val itemTimeSecond = item.first * 1000
                    val noDvrSeconds = (itemTimeSecond - itemTimeFirst) / 1000

                    val secondsToStartPoint = (timeFirst - itemTimeFirst) / 1000
                    val paddingTop = secondsToStartPoint * heightInOneSecond
                    val strokeWidth = heightInOneSecond * noDvrSeconds

//                    Timber.d("ITEWMGETLINET strokeWidth childCount $childCount $strokeWidth paddingTop $paddingTop")
                    if (view != null) {
                        drawLine(c, view, strokeWidth, paddingTop)
                    }
                }
            }
        }


//        mExoPlayerViewModel.getTimeLineItem().value?.rangeTime!![firstVisibleItem][timeFirst]?.forEach { item ->
//            if (item != null) {
////                val itemTimeFirst = item.first * 1000
//                val itemTimeFirst = item.second * 1000
////                val itemTimeSecond = item.second * 1000
//                val itemTimeSecond = item.first * 1000
//                val noDvrSeconds = (itemTimeSecond - itemTimeFirst) / 1000
//
//                val secondsToStartPoint = (timeFirst - itemTimeFirst) / 1000
//                val paddingTop = secondsToStartPoint * heightInOneSecond
//                val strokeWidth = heightInOneSecond * noDvrSeconds
//
//                Timber.d("ITEWMGETLINET strokeWidth $strokeWidth paddingTop $paddingTop")
//                drawLine(c, firstVisibleView, strokeWidth, paddingTop)
//            }
//        }
    }

    private fun drawLine(canvas: Canvas, child: View, strokeWidth: Float, yPadding: Float) {
        val params = child.layoutParams as RecyclerView.LayoutParams

        val startX = child.right
        val startY = child.top + yPadding
        val stopX = child.left
        val stopY = startY
//        child.setBackgroundColor(Color.GREEN)
        linePaint.strokeWidth = strokeWidth
        Timber.d("ITEWMGETLINET startX ${startX} startY $startY child.top ${child.top} stopX $stopX stopY $stopY CHILD TOP +  ${child.top } ${child.bottom}")

        canvas.drawLine(startX.toFloat(), startY, stopX.toFloat(), stopY, linePaint)
    }


}
