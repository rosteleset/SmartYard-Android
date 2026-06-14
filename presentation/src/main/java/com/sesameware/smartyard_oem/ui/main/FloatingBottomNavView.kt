package com.sesameware.smartyard_oem.ui.main

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import com.sesameware.smartyard_oem.R

class FloatingBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var colorActive = "#007AFF".toColorInt()
    private var colorInactive = "#8E8E93".toColorInt()
    private val glassColor = "#E6FFFFFF".toColorInt()
    private val indicatorColor = "#26007AFF".toColorInt()
    private val badgeColor = "#FF3B30".toColorInt()

    private val indicatorView: View
    private val tabsContainer: LinearLayout

    private val itemIds = mutableListOf<Int>()
    private val tabViews = mutableListOf<TabViewHolder>()
    private var _selectedItemId: Int = -1
    var selectedItemId: Int
        set(value) { setSelection(value) }
        get() = _selectedItemId

    private val onItemSelectedListeners: MutableSet<(Int) -> Unit> = mutableSetOf()
    private val onItemReselectedListeners: MutableSet<(Int) -> Unit> = mutableSetOf()

    init {
        background = createRoundRect(glassColor, 24f, "#1A000000".toColorInt())
        elevation = dpToPx(8f)
        clipChildren = false
        clipToPadding = false

        indicatorView = View(context).apply {
            background = createRoundRect(indicatorColor, 18f)
        }
        addView(indicatorView, LayoutParams(0, LayoutParams.MATCH_PARENT))

        tabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 0f
            clipChildren = false
            clipToPadding = false
        }
        addView(tabsContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FloatingBottomNavView) {

                colorActive = getColor(R.styleable.FloatingBottomNavView_activeColor, colorActive)
                colorInactive = getColor(R.styleable.FloatingBottomNavView_inactiveColor, colorInactive)

                val menuRes = getResourceId(R.styleable.FloatingBottomNavView_menu, 0)
                if (menuRes != 0) {
                    inflateMenu(menuRes)
                }
            }
        }
    }

    fun addOnItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListeners.add(listener)
    }

    fun removeOnItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListeners.remove(listener)
    }

    fun removeAllOnItemSelectedListeners() {
        onItemSelectedListeners.clear()
    }

    fun addOnItemReselectedListener(listener: (Int) -> Unit) {
        onItemReselectedListeners.add(listener)
    }

    fun removeOnItemReselectedListener(listener: (Int) -> Unit) {
        onItemReselectedListeners.remove(listener)
    }

    fun removeAllOnItemReselectedListeners() {
        onItemReselectedListeners.clear()
    }

    fun showDotBadge(id: Int) {
        val index = itemIds.indexOf(id)
        if (index == -1) return
        val badge = tabViews[index].badge

        badge.text = ""
        badge.layoutParams = (badge.layoutParams as LayoutParams).apply {
            width = dpToPx(10f).toInt()
            height = dpToPx(10f).toInt()
            topMargin = dpToPx(4f).toInt()
            rightMargin = dpToPx(8f).toInt()
        }
        badge.setPadding(0, 0, 0, 0)
        badge.isVisible = true
    }

    fun showNumberBadge(id: Int, count: Int) {
        val index = itemIds.indexOf(id)
        if (index == -1) return
        val badge = tabViews[index].badge

        if (count <= 0) {
            clearBadge(id)
            return
        }

        badge.text = if (count > 99) "99+" else count.toString()
        badge.layoutParams = (badge.layoutParams as LayoutParams).apply {
            width = LayoutParams.WRAP_CONTENT
            height = dpToPx(16f).toInt()
            topMargin = dpToPx(2f).toInt()
            rightMargin = dpToPx(4f).toInt()
        }
        badge.minWidth = dpToPx(16f).toInt()
        val padding = dpToPx(4f).toInt()
        badge.setPadding(padding, 0, padding, 0)
        badge.isVisible = true
    }

    fun clearBadge(@IdRes itemId: Int) {
        val index = itemIds.indexOf(itemId)
        if (index == -1) return
        tabViews[index].badge.isVisible = false
    }

    fun inflateMenu(@MenuRes menuRes: Int) {
        val popup = PopupMenu(context, this)
        popup.inflate(menuRes)
        val menu = popup.menu
        for (i in 0 until menu.size) {
            val item = menu[i]
            addItem(item.itemId, item.icon, item.title)
        }
    }

    fun addItem(@IdRes itemId: Int, icon: Drawable?, title: CharSequence? = null) {
        tabsContainer.weightSum = tabsContainer.weightSum + 1f

        val tabLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { setSelection(itemId, animate = true) }
            clipChildren = false
        }

        val iconContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            clipChildren = false
        }

        val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(dpToPx(24f).toInt(), dpToPx(24f).toInt()).apply {
                gravity = Gravity.CENTER
                setMargins(dpToPx(12f).toInt(), dpToPx(4f).toInt(), dpToPx(12f).toInt(), dpToPx(2f).toInt())
            }
            setImageDrawable(icon)
            setColorFilter(colorInactive)
        }

        val badgeView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END
            }
            background = createBadgeDrawable()
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            includeFontPadding = false
            typeface = Typeface.DEFAULT_BOLD
            isVisible = false
            elevation = dpToPx(2f)
        }

        iconContainer.addView(imageView)
        iconContainer.addView(badgeView)
        tabLayout.addView(iconContainer)

        var titleTextView: TextView? = null
        if (!title.isNullOrEmpty()) {
            titleTextView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = title
                textSize = 10f
                setTextColor(colorInactive)
                isSingleLine = true
            }
            tabLayout.addView(titleTextView)
        }

        tabsContainer.addView(tabLayout)
        itemIds.add(itemId)
        tabViews.add(TabViewHolder(imageView, titleTextView, badgeView))

        if (itemIds.size == 1) {
            setSelection(itemId, animate = false)
        } else if (!isInEditMode) {
            post { updateIndicatorLayout(animate = false) }
        }
    }

    fun hasItem(@IdRes itemId: Int): Boolean = itemIds.contains(itemId)

    fun setSelection(@IdRes itemId: Int, animate: Boolean = true, notify: Boolean = true): Boolean {
        val index = itemIds.indexOf(itemId)
        if (index == -1) return false

        if (itemId == selectedItemId) {
            if (notify) {
                onItemReselectedListeners.forEach { listener ->
                    listener.invoke(itemId)
                }
            }
            return false
        }

        _selectedItemId = itemId

        if (notify) {
            onItemSelectedListeners.forEach { listener ->
                listener.invoke(itemId)
            }
        }

        tabViews.forEachIndexed { i, tab ->
            val color = if (i == index) colorActive else colorInactive
            tab.icon.setColorFilter(color)
            tab.text?.setTextColor(color)
        }

        updateIndicatorLayout(animate)

        return true
    }

    fun removeItem(@IdRes itemId: Int) {
        val index = itemIds.indexOf(itemId)
        if (index == -1) return

        tabsContainer.weightSum -= 1f
        tabsContainer.removeViewAt(index)
        itemIds.removeAt(index)
        tabViews.removeAt(index)

        if (selectedItemId == itemId) {
            setSelection(itemIds.firstOrNull() ?: -1, animate = true)
        }
        if (!isInEditMode) post { updateIndicatorLayout(animate = true) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateIndicatorLayout(animate = false)
    }

    private fun updateIndicatorLayout(animate: Boolean) {
        if (itemIds.isEmpty() || width == 0) return

        val index = itemIds.indexOf(selectedItemId)
        if (index == -1) return

        val tabWidth = width / itemIds.size.toFloat()
        val margin = dpToPx(6f)

        val lp = indicatorView.layoutParams as LayoutParams
        lp.width = (tabWidth - margin * 2).toInt()

        lp.height = height - (margin * 2).toInt()
        lp.topMargin = margin.toInt()
        indicatorView.layoutParams = lp

        val targetX = (index * tabWidth) + margin
        if (animate && !isInEditMode) {
            indicatorView.animate()
                .translationX(targetX)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        } else {
            indicatorView.translationX = targetX
        }

        if (isInEditMode) {
            indicatorView.layout(0, lp.topMargin, lp.width, lp.topMargin + lp.height)
        }
    }

    private fun createRoundRect(color: Int, radiusDp: Float, strokeColor: Int = Color.TRANSPARENT): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dpToPx(radiusDp)
            if (strokeColor != Color.TRANSPARENT) setStroke(dpToPx(1f).toInt(), strokeColor)
        }
    }

    private fun createBadgeDrawable(): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(badgeColor)
            cornerRadius = dpToPx(16f)
            setStroke(dpToPx(1.5f).toInt(), Color.WHITE)
        }
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private data class TabViewHolder(val icon: ImageView, val text: TextView?, val badge: TextView)
}