package com.sesameware.smartyard_oem.ui.main.address.event_log

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.getColorCompat
import kotlin.math.min

class FaceImageView : AppCompatImageView {

    // для лиц
    private lateinit var registeredPaint: Paint
    private lateinit var unregisteredPaint: Paint
    private var faceLeft = -1.0f
    private var faceTop = -1.0f
    private var faceWidth = -1.0f
    private var faceHeight = -1.0f
    private var isRegistered = false

    // для автомобильных номеров
    private lateinit var vehicleBoxPaint: Paint
    private lateinit var plateKeyPointsPaint: Paint
    private var vehicleBox = mutableListOf<Int>()
    private var plateKeyPoints = mutableListOf<Int>()

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init(context)
    }

    private fun init(context: Context) {
        registeredPaint = Paint().apply {
            color = context.resources.getColorCompat(R.color.green_100)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.event_log_detail_stroke_size).toFloat()
            style = Paint.Style.STROKE
        }
        unregisteredPaint = Paint().apply {
            color = context.resources.getColorCompat(R.color.red_100)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.event_log_detail_stroke_size).toFloat()
            style = Paint.Style.STROKE
        }
        vehicleBoxPaint = Paint().apply {
            color = context.resources.getColorCompat(R.color.vehicle_box)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.event_log_detail_stroke_size).toFloat()
            style = Paint.Style.STROKE
        }
        plateKeyPointsPaint = Paint().apply {
            color = context.resources.getColorCompat(R.color.plate_key_points)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.event_log_detail_stroke_size).toFloat()
            style = Paint.Style.STROKE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val iw = drawable?.intrinsicWidth ?: 0
        val ih = drawable?.intrinsicHeight ?: 0
        if (iw == 0 || ih == 0) {
            return
        }

        val scaleX = measuredWidth.toFloat() / iw.toFloat()
        val scaleY = measuredHeight.toFloat() / ih.toFloat()

        if (faceWidth > 0 && faceHeight > 0) {
            //прямоугольник
            /*canvas?.drawRect(
                faceLeft * scaleX,
                faceTop * scaleY,
                (faceLeft + faceWidth - 1) * scaleX,
                (faceTop + faceHeight - 1) * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)*/

            //8 линий
            val lineLength = min(faceWidth, faceHeight) / 4
            canvas.drawLine(faceLeft * scaleX, faceTop * scaleY, (faceLeft + lineLength) * scaleX, faceTop * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawLine(faceLeft * scaleX, faceTop * scaleY, faceLeft * scaleX, (faceTop + lineLength)  * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawRect(faceLeft * scaleX, faceTop * scaleY, faceLeft * scaleX, faceTop * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)

            canvas.drawLine((faceLeft + faceWidth - 1) * scaleX, faceTop * scaleY, (faceLeft + faceWidth - 1 - lineLength) * scaleX, faceTop * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawLine((faceLeft + faceWidth - 1) * scaleX, faceTop * scaleY, (faceLeft + faceWidth - 1) * scaleX, (faceTop + lineLength)  * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawRect((faceLeft + faceWidth - 1) * scaleX, faceTop * scaleY, (faceLeft + faceWidth - 1) * scaleX, faceTop * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)

            canvas.drawLine(faceLeft * scaleX, (faceTop + faceHeight - 1) * scaleY, (faceLeft + lineLength) * scaleX, (faceTop + faceHeight - 1) * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawLine(faceLeft * scaleX, (faceTop + faceHeight - 1) * scaleY, faceLeft * scaleX, (faceTop + faceHeight - 1 - lineLength)  * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawRect(faceLeft * scaleX, (faceTop + faceHeight - 1) * scaleY, faceLeft * scaleX, (faceTop + faceHeight - 1) * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)

            canvas.drawLine((faceLeft + faceWidth - 1) * scaleX, (faceTop + faceHeight - 1) * scaleY, (faceLeft + faceWidth - 1 - lineLength) * scaleX, (faceTop + faceHeight - 1) * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawLine((faceLeft + faceWidth - 1) * scaleX, (faceTop + faceHeight - 1) * scaleY, (faceLeft + faceWidth - 1) * scaleX, (faceTop + faceHeight - 1 - lineLength)  * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
            canvas.drawRect((faceLeft + faceWidth - 1) * scaleX, (faceTop + faceHeight - 1) * scaleY, (faceLeft + faceWidth - 1) * scaleX, (faceTop + faceHeight - 1) * scaleY,
                if (isRegistered) registeredPaint else unregisteredPaint)
        }

        if (vehicleBox.size == 4) {
            // прямоугольник машины
            canvas.drawRect(
                vehicleBox[0] * scaleX,
                vehicleBox[1] * scaleY,
                vehicleBox[2] * scaleX,
                vehicleBox[3] * scaleY,
                vehicleBoxPaint)
        }

        if (plateKeyPoints.size == 8) {
            // 4 линии номера машины
            canvas.drawLine(plateKeyPoints[0] * scaleX, plateKeyPoints[1] * scaleY, plateKeyPoints[2] * scaleX, plateKeyPoints[3] * scaleY, plateKeyPointsPaint)
            canvas.drawLine(plateKeyPoints[2] * scaleX, plateKeyPoints[3] * scaleY, plateKeyPoints[4] * scaleX, plateKeyPoints[5] * scaleY, plateKeyPointsPaint)
            canvas.drawLine(plateKeyPoints[4] * scaleX, plateKeyPoints[5] * scaleY, plateKeyPoints[6] * scaleX, plateKeyPoints[7] * scaleY, plateKeyPointsPaint)
            canvas.drawLine(plateKeyPoints[6] * scaleX, plateKeyPoints[7] * scaleY, plateKeyPoints[1] * scaleX, plateKeyPoints[1] * scaleY, plateKeyPointsPaint)
        }
    }

    fun setFaceRect(fLeft: Int, fTop: Int, fWidth: Int, fHeight: Int, isReg: Boolean) {
        faceLeft = fLeft.toFloat()
        faceTop = fTop.toFloat()
        faceWidth = fWidth.toFloat()
        faceHeight = fHeight.toFloat()
        isRegistered = isReg
        invalidate()
    }

    fun setVehicleData(vBox: MutableList<Int>, kPoints: MutableList<Int>) {
        vehicleBox = vBox
        plateKeyPoints = kPoints
    }
}
