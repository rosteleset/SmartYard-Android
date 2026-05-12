package com.sesameware.smartyard_oem.ui.common.fixedlistview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.sesameware.smartyard_oem.R
import java.util.concurrent.Executors

class FixedListView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private companion object {
        private const val MAX_ITEM_COUNT = 10
    }

    private lateinit var container: LinearLayout
    private var currentList: List<FixedListItem> = emptyList()

    private val binders = mutableMapOf<Class<out FixedListItem>, FixedListBinder<*>>()

    private val diffExecutor = Executors.newSingleThreadExecutor()

    private var isInitialized = false
    private var delayedList: List<FixedListItem>? = null

    init {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.orientation))
        val orientation = typedArray.getInt(0, LinearLayout.VERTICAL)
        typedArray.recycle()

        if (orientation == LinearLayout.HORIZONTAL) {
            createAndAttachHorizontalView()
        } else {
            createAndAttachVerticalView()
        }

        if (isInEditMode) {
            handleEditMode(attrs)
        }

        onInitialized()
    }

    private fun createAndAttachHorizontalView() {
        val root = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            isHorizontalScrollBarEnabled = false
        }

        val ll = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
        }

        addView(root)
        root.addView(ll)
        container = ll
    }

    private fun createAndAttachVerticalView() {
        val root = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            isVerticalScrollBarEnabled = false
        }

        val ll = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
        }

        addView(root)
        root.addView(ll)
        container = ll
    }

    private fun onInitialized() {
        isInitialized = true

        delayedList?.let {submitList(it)}
    }

    /**
     * Sets a list of binders for different data types.
     */
    fun <T : FixedListItem> registerBinders(vararg newBinders: FixedListBinder<out T>) {
        binders.clear()

        newBinders.forEach { binder -> binders[binder.itemType] = binder }
    }

    /**
     * Sends a new list of data to be displayed.
     * Calculates the difference with the old list using DiffUtil and updates the UI.
     * Throws an IllegalArgumentException, if the number of elements is greater than MAX_ITEM_COUNT,
     */
    fun <T : FixedListItem> submitList(newList: List<T>) {

        require(newList.size <= MAX_ITEM_COUNT) {
            "List size must be less or equal to $MAX_ITEM_COUNT. Passed list size is ${newList.size}"
        }

        if (!isInitialized) {
            delayedList = newList
            return
        }

        diffExecutor.execute {
            val diffCallback = FixedListDiffCallback(currentList, newList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            post {
                currentList = newList
                diffResult.dispatchUpdatesTo(listUpdateCallback)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBinderForItem(item: FixedListItem) =
        binders[item::class.java] as? FixedListBinder<FixedListItem>
            ?: throw IllegalStateException(
                "Не найден адаптер для типа ${item::class.java.simpleName}. " +
                "Вы зарегистрировали его через registerBinders()?"
            )

    private fun createViewForItem(item: FixedListItem) =
        getBinderForItem(item).inflate(container)

    private fun bindViewWithItem(view: View, item: FixedListItem, animate: Boolean = false) =
        getBinderForItem(item).bind(view, item)

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            for (i in 0 until count) {
                val item = currentList[position + i]
                val view = createViewForItem(item)
                bindViewWithItem(view, item)
                container.addView(view, position + i)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            // Removing from the end so as not to break the indexes
            for (i in (count - 1) downTo 0) {
                container.removeViewAt(position + i)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            // Implementing the move as "delete and add" so that the built-in animation works.
            val view = container.getChildAt(fromPosition)
            container.removeViewAt(fromPosition)
            container.addView(view, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            for (i in 0 until count) {
                val item = currentList[position + i]
                val view = container.getChildAt(position + i)
                bindViewWithItem(view, item, true)
                container.addView(view, position + i)
            }
        }
    }

    private fun handleEditMode(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.FixedListView) {
            val itemLayout = getResourceId(R.styleable.FixedListView_tools_listItem, 0)
            val itemCount = getInteger(R.styleable.FixedListView_tools_itemCount, 0)

            if (itemLayout != 0) {
                for (i in 0..<itemCount) {
                    val view = LayoutInflater.from(context).inflate(itemLayout, container, false)
                    container.addView(view)
                }
            }
        }
    }

    private class FixedListDiffCallback(
        private val oldList: List<FixedListItem>,
        private val newList: List<FixedListItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos]::class.java == newList[newPos]::class.java &&
                oldList[oldPos].distinctiveFieldEquals(newList[newPos])

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }
}