package com.sesameware.smartyard_oem.ui.main

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath
import com.sesameware.smartyard_oem.ui.main.MagicShapePath.CircleShape
import com.sesameware.smartyard_oem.ui.main.MagicShapePath.PathDirection
import com.sesameware.smartyard_oem.ui.main.MagicShapePath.ShiftMode
import kotlin.math.abs

class MorphBottomNavigationViewTopEdgeTreatment(
    private val bottomNavigationMenuView: BottomNavigationMenuView,
    var morphItemRadius: Float,
    var morphVerticalOffset: Float,
    var morphCornerRadius: Float
) :
    EdgeTreatment() {

    private lateinit var easyShapePath: MagicShapePath

    var lastSelectedItem: Int = 0
    var selectedItem: Int = 0

    override fun getEdgePath(
        length: Float,
        center: Float,
        interpolation: Float,
        shapePath: ShapePath
    ) {
        val easyShapePath2 =
            MagicShapePath.create(
                0f,
                0f /*morphVerticalOffset*/,
                length,
                0f /*morphVerticalOffset*/
            )

        var doAddPath = false

        bottomNavigationMenuView.forEachIndexed { i, view ->
            //Расчёты делаем для текущего и предыдущего выделенного элемента.
            //Анимация предыдущего выделенного элемента делается, если он не соседний с текущим, иначе
            //появляется визуальный глюк в виде моргающего круга, по крайней мере, на Андроидах 11+
            if (view.isVisible && (i == selectedItem ||
                        (i == lastSelectedItem && abs(lastSelectedItem - selectedItem) > 1))
            ) {
                val morphHeightOffset = if (i == selectedItem) {
                    1.0f * morphVerticalOffset
                } else {
                    (1 - interpolation) * morphVerticalOffset
                }

                val itemRect = view.globalVisibleRect
                if (itemRect.height() > 0) {
                    doAddPath = true

                    val centerRadius = morphItemRadius
                    val borderRadius = morphCornerRadius
                    val centerX = itemRect.centerX().toFloat()
                    val centerY = /*morphVerticalOffset + */centerRadius - morphHeightOffset

                    val centerCircle =
                        CircleShape(centerX, centerY, centerRadius, PathDirection.CLOCKWISE)

                    val leftCircle = CircleShape(
                        centerX, /*morphVerticalOffset*/
                        -borderRadius,
                        borderRadius,
                        PathDirection.C_CLOCKWISE
                    )
                    centerCircle.shiftOutside(leftCircle, ShiftMode.LEFT)

                    val rightCircle = CircleShape(
                        centerX, /*morphVerticalOffset*/
                        -borderRadius,
                        borderRadius,
                        PathDirection.C_CLOCKWISE
                    )
                    centerCircle.shiftOutside(rightCircle, ShiftMode.RIGHT)

                    easyShapePath2.addCircles(leftCircle, centerCircle, rightCircle)
                }
            }
        }

        if (doAddPath) {
            easyShapePath = easyShapePath2
        }
        if (this::easyShapePath.isInitialized) {
            easyShapePath.applyOn(shapePath)
        }
    }

    fun drawDebug(canvas: Canvas, paint: Paint) {
        easyShapePath.drawDebug(canvas, paint)
    }

    private inline val View.globalVisibleRect: Rect
        get() {
            val r = Rect()
            getGlobalVisibleRect(r)
            return r
        }

}
