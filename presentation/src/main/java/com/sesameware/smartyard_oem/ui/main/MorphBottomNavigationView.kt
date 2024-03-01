package com.sesameware.smartyard_oem.ui.main

import com.sesameware.smartyard_oem.R
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener
import com.google.android.material.shape.*

class MorphEdgeTreatment : EdgeTreatment() {
  override fun getEdgePath(length: Float,
    center: Float,
    interpolation: Float,
    shapePath: ShapePath
  ) {
    shapePath.lineTo(0f, -10f)
    shapePath.lineTo(length, -10f)
  }
}

class MorphBottomNavigationView : BottomNavigationView, OnNavigationItemSelectedListener {
  private val paint = Paint().apply {

    isAntiAlias = true
    color = Color.BLUE
    strokeWidth = 2f
    style = STROKE
    alpha = 50
  }
  private val height: Float

  private val topEdgeTreatment: MorphBottomNavigationViewTopEdgeTreatment
  private val materialShapeDrawable: MaterialShapeDrawable

  private var selectedItem = 0

  private var selectionAnimator: ValueAnimator? = null

  private var morphItemRadius: Float = 0f
    set(radius) {
      field = radius
      topEdgeTreatment.morphItemRadius = radius
      invalidate()
    }

  private var morphCornerRadius: Float = 0f
    set(radius) {
      field = radius
      topEdgeTreatment.morphCornerRadius = radius
      invalidate()
    }

  private var morphVerticalOffset: Float = 0f
    set(offset) {
      field = offset
      topEdgeTreatment.morphVerticalOffset = offset
      if (layoutParams != null) {
        layoutParams.height = (height + morphVerticalOffset).toInt()
      }

      invalidate()
    }

  private var drawDebug: Boolean = false
    set(enable) {
      field = enable
      invalidate()
    }

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

  private var bottomNavigationMenuView: BottomNavigationMenuView

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
          defStyleAttr) {

    val a = resources.obtainAttributes(attrs, R.styleable.MorphBottomNavigationView)

    val typedValue = TypedValue()
    context.theme?.resolveAttribute(R.attr.colorPrimary, typedValue, true)
    val backgroundTint = a.getColor(R.styleable.MorphBottomNavigationView_backgroundTint, typedValue.data)

    val morphItemRadius = a.getDimensionPixelSize(R.styleable.MorphBottomNavigationView_morphItemRadius, dpToPx(64f).toInt()).toFloat()
    val morphVerticalOffset = a.getDimensionPixelSize(R.styleable.MorphBottomNavigationView_morphVerticalOffset, dpToPx(8f).toInt()).toFloat()
    val morphCornerRadius = a.getDimensionPixelSize(R.styleable.MorphBottomNavigationView_morphCornerRadius, dpToPx(128f).toInt()).toFloat()
    a.recycle()

    height = resources.getDimension(R.dimen.design_bottom_navigation_height)
    bottomNavigationMenuView = getChildAt(0) as BottomNavigationMenuView
    topEdgeTreatment = MorphBottomNavigationViewTopEdgeTreatment(
            bottomNavigationMenuView,
            morphItemRadius,
            morphVerticalOffset,
            morphCornerRadius)

    this.morphItemRadius = morphItemRadius
    this.morphVerticalOffset = morphVerticalOffset
    this.morphCornerRadius = morphCornerRadius

    val shapePathModel = ShapeAppearanceModel
      .builder()
      .setTopEdge(topEdgeTreatment)
      .setLeftEdge(MorphEdgeTreatment()) //левая, правая и нижняя границы за пределом экрана
      .setRightEdge(MorphEdgeTreatment())
      .setBottomEdge(MorphEdgeTreatment())
      .build()
    materialShapeDrawable = MaterialShapeDrawable(shapePathModel)
    materialShapeDrawable.shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_NEVER
    materialShapeDrawable.paintStyle = FILL_AND_STROKE
    materialShapeDrawable.setStroke(4.0f, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)))
    materialShapeDrawable.setTint(backgroundTint)

    background = materialShapeDrawable
    val menuParams = bottomNavigationMenuView.layoutParams as FrameLayout.LayoutParams
    menuParams.gravity = (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)

    setOnNavigationItemSelectedListener(this)

    setWillNotDraw(false)
  }

  /**
   * Proxy for listener
   */
  @Deprecated("Deprecated in Java")
  override fun setOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener?) {
    super.setOnNavigationItemSelectedListener {
      onNavigationItemSelected(it)
      if (listener !is MorphBottomNavigationView) {
        listener?.onNavigationItemSelected(it)
      }
      true
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val indexOfItemSelected = menu.indexOfItem(item)
    if (indexOfItemSelected != selectedItem) {
      topEdgeTreatment.lastSelectedItem = selectedItem
      topEdgeTreatment.selectedItem = indexOfItemSelected
      selectedItem = indexOfItemSelected

      selectionAnimator?.end()
      selectionAnimator?.cancel()

      selectionAnimator = ValueAnimator.ofFloat(0f, 1f)
      selectionAnimator?.addUpdateListener {
        materialShapeDrawable.interpolation = it.animatedValue as Float
      }

      selectionAnimator?.duration = 200
      selectionAnimator?.interpolator = DecelerateInterpolator()

      selectionAnimator?.start()
    }

    return true
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    // Change height
    layoutParams.height = (height + morphVerticalOffset).toInt()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (drawDebug) {
      topEdgeTreatment.drawDebug(canvas, paint)
    }
  }

  private fun dpToPx(dp: Float): Float {
    return resources.displayMetrics.density * dp
  }

  private fun Menu.indexOfItem(item: MenuItem): Int {
    for (i in 0 until this.size()) {
      val menuItem = menu.getItem(i)
      if (menuItem.itemId == item.itemId) {
        return i
      }
    }
    return -1
  }
}
