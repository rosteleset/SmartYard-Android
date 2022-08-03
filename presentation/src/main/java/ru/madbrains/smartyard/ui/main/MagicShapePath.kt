package ru.madbrains.smartyard.ui.main

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style.FILL
import android.graphics.Paint.Style.STROKE
import android.graphics.RectF
import com.google.android.material.shape.ShapePath
import ru.madbrains.smartyard.ui.main.MagicShapePath.PathDirection.CLOCKWISE
import ru.madbrains.smartyard.ui.main.MagicShapePath.PathDirection.C_CLOCKWISE
import ru.madbrains.smartyard.ui.main.MagicShapePath.ShiftMode.LEFT
import ru.madbrains.smartyard.ui.main.MagicShapePath.ShiftMode.RIGHT
import ru.madbrains.smartyard.ui.main.MagicShapePath.ShiftMode.TOP
import java.util.ArrayList

class MagicShapePath private constructor(private val startX: Float, private val startY: Float,
    private val endX: Float, private val endY: Float) {

    private val circles: ArrayList<CircleShape> = ArrayList()

    companion object {
        fun create(startX: Float, startY: Float, endX: Float, endY: Float) =
            MagicShapePath(startX, startY, endX, endY)
    }

    fun applyOn(shapePath: ShapePath) {
        var px = startX
        var py = 0f /*startY*/

        shapePath.lineTo(px, py)

        for (i in 0 until circles.size) {
            val circle = circles[i]
            val nextCircle = if (i < circles.size - 1) circles[i + 1] else null

            val (tPx, tPy) = getLineToCircle(px, py, circle)
            shapePath.lineTo(tPx.toFloat(), tPy.toFloat())

            var startAngle: Double
            var endAngle: Double

            // Calculate the common tangent between the current and the next circle
            if (nextCircle != null) {
                // All tangents between current circle and next circle
                val tangent: DoubleArray = getTangentForTwoCircles(circle, nextCircle)

                startAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tPx,
                    tPy)
                endAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tangent[0],
                    tangent[1])

                px = tangent[0].toFloat()
                py = tangent[1].toFloat()
            } else {
                // Draw last arc
                val tangentOfLastPointToCircle = getTangentForPointToCircle(circle)

                startAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tPx,
                    tPy)
                endAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tangentOfLastPointToCircle[2],
                    tangentOfLastPointToCircle[3])
            }

            var sweepAngle = endAngle - startAngle
            if (circle.pathDirection == CLOCKWISE && sweepAngle < 0) {
                sweepAngle += 360
            } else if (circle.pathDirection == C_CLOCKWISE && sweepAngle > 0) {
                sweepAngle -= 360
            }

            shapePath.addArc(circle.left,
                circle.top,
                circle.right,
                circle.bottom,
                startAngle.toFloat(),
                sweepAngle.toFloat())
        }

        shapePath.lineTo(endX, 0f /*endY*/)
    }

    private fun getTangentForTwoCircles(circle: CircleShape, nextCircle: CircleShape): DoubleArray {
        val tangents = GeometryUtils.getTangentsOfTwoCircles(
            circle.centerX.toDouble(),
            circle.centerY.toDouble(),
            circle.radius.toDouble(),
            nextCircle.centerX.toDouble(),
            nextCircle.centerY.toDouble(),
            nextCircle.radius.toDouble())

        return if (circle.pathDirection == nextCircle.pathDirection) {
            if (circle.pathDirection == C_CLOCKWISE) tangents[0] else tangents[1]
        } else {
            if (circle.pathDirection == C_CLOCKWISE) tangents[2] else tangents[3]
        }
    }

    fun addCircles(vararg circles: CircleShape) {
        this.circles.addAll(circles)
    }

    fun drawDebug(canvas: Canvas, paint: Paint) {
        var px = startX
        var py = startY

        for (i in 0 until circles.size) {
            val circle = circles[i]
            val nextCircle = if (i < circles.size - 1) circles[i + 1] else null

            paint.alpha = 50
            paint.style = STROKE
            canvas.drawCircle(circle.centerX, circle.centerY, circle.radius, paint)

            val (tPx, tPy) = getLineToCircle(px, py, circle)

            paint.alpha = 255
            paint.style = FILL
            canvas.drawCircle(tPx.toFloat(), tPy.toFloat(), 2f, paint)

            var startAngle: Double
            var endAngle: Double

            // Calculate the common tangent between the current and the next circle
            if (nextCircle != null) {
                // All tangents between current circle and next circle
                val tangent: DoubleArray = getTangentForTwoCircles(circle, nextCircle)

                startAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tPx,
                    tPy)
                endAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tangent[0],
                    tangent[1])

                paint.alpha = 255
                paint.style = FILL
                canvas.drawCircle(tangent[0].toFloat(), tangent[1].toFloat(), 2f, paint)

                px = tangent[0].toFloat()
                py = tangent[1].toFloat()
            } else {
                // Draw last arc
                val tangentOfLastPointToCircle = getTangentForPointToCircle(circle)
                paint.alpha = 255
                paint.style = FILL
                canvas.drawCircle(tangentOfLastPointToCircle[2].toFloat(),
                    tangentOfLastPointToCircle[3].toFloat(),
                    2f,
                    paint)

                startAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tPx,
                    tPy)
                endAngle = GeometryUtils.angleBetweenPoints(circle.centerX.toDouble(),
                    circle.centerY.toDouble(),
                    tangentOfLastPointToCircle[2],
                    tangentOfLastPointToCircle[3])
            }

            var sweepAngle = endAngle - startAngle
            if (circle.pathDirection == CLOCKWISE && sweepAngle < 0) {
                sweepAngle += 360
            } else if (circle.pathDirection == C_CLOCKWISE && sweepAngle > 0) {
                sweepAngle -= 360
            }

            paint.alpha = 50
            paint.style = STROKE
            canvas.drawArc(RectF(circle.left,
                circle.top,
                circle.right,
                circle.bottom),
                startAngle.toFloat(),
                sweepAngle.toFloat(),
                true,
                paint)
        }
    }

    private fun getTangentForPointToCircle(circle: CircleShape): DoubleArray {
        val tangentsOfLastPointToCircle = GeometryUtils.getTangentsOfPointToCircle(
            endX.toDouble(),
            endY.toDouble(),
            circle.centerX.toDouble(),
            circle.centerY.toDouble(),
            circle.radius.toDouble())
        return tangentsOfLastPointToCircle[if (circle.pathDirection == C_CLOCKWISE) 0 else 1]
    }

    private fun getLineToCircle(px: Float, py: Float,
        circle: CircleShape): Pair<Double, Double> {
        val tangentsOfPointToCircle = GeometryUtils.getTangentsOfPointToCircle(
            px.toDouble(),
            py.toDouble(),
            circle.centerX.toDouble(),
            circle.centerY.toDouble(),
            circle.radius.toDouble())
        val tangentOfPointToCircle =
            tangentsOfPointToCircle[if (circle.pathDirection == CLOCKWISE) 0 else 1]

        val tPx = tangentOfPointToCircle[2]
        val tPy = tangentOfPointToCircle[3]
        return Pair(tPx, tPy)
    }

    open class CircleShape(var centerX: Float,
        val centerY: Float,
        val radius: Float,
        val pathDirection: PathDirection) {
        fun shiftOutside(circle: CircleShape, shift: ShiftMode) {
            if (shift == LEFT || shift == RIGHT) {
                val difY = circle.centerY - centerY
                val distance = circle.radius + radius

                val difX = Math.sqrt((distance * distance - difY * difY).toDouble()) + 0.1
                val sign = if (shift == LEFT) -1 else 1
                circle.centerX = (centerX + difX * sign).toFloat()
            } else {
                val difX = circle.centerX - centerX
                val distance = circle.radius + radius

                val difY = Math.sqrt((distance * distance - difX * difX).toDouble()) + 0.1
                val sign = if (shift == TOP) -1 else 1
                circle.centerX = (centerX + difY * sign).toFloat()
            }
        }

        val top: Float get() = centerY - radius
        val left: Float get() = centerX - radius
        val bottom: Float get() = centerY + radius
        val right: Float get() = centerX + radius
    }

    enum class ShiftMode {
        LEFT, RIGHT, TOP, BOTTOM
    }

    enum class PathDirection {
        CLOCKWISE, C_CLOCKWISE
    }
}
