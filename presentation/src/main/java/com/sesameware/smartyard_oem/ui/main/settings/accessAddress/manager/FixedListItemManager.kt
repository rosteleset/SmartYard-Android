package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.manager

import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter.FixedListItemBinder

class FixedListItemManager<T> {

    constructor(
        ll: LinearLayout,
        vararg binders: FixedListItemBinder<T>
    ) {
        this.ll = ll
        this.binders = binders
        populateLayout()
    }

    private fun populateLayout() {
        val transition = ll.layoutTransition
        ll.layoutTransition = null
        ll.removeAllViews()
        binders.forEach { binder ->
            repeat(binder.itemCount) {
                binder.inflate(ll)
            }
        }
        ll.layoutTransition = transition
    }

    private val ll: LinearLayout
    private val binders: Array<out FixedListItemBinder<T>>

    fun submitList(vararg newLists: List<T>) {
        require(newLists.size == binders.size) { """
            The number of arguments passed does not match the number of registered binders.
            lists passed: ${newLists.size}, binders registered ${binders.size}
        """.trimIndent() }
        moveViews(newLists)
        showOrHideViews(newLists.map { it.size })
        rebindViews(newLists)
    }

    private fun showOrHideViews(sizes: List<Int>) {
        var acc = 0
        for (i in sizes.indices) {
            val itemCount = binders[i].itemCount
            val listSize = sizes[i]
            require(listSize <= itemCount) { """
                The size of each list must be less than or equal
                to the number of views of the corresponding type.
                List #$i size is $listSize, item count of $i view type is $itemCount
            """.trimIndent() }
            for (j in acc..<listSize + acc) {
                ll.getChildAt(j).isVisible = true
            }
            for (j in listSize + acc..<itemCount + acc) {
                ll.getChildAt(j).isVisible = false
            }
            acc += itemCount
        }
    }

    private fun moveViews(lists: Array<out List<T>>) {
        var acc = 0
        for (i in lists.indices) {
            lists[i].forEachIndexed { j, item ->
                for (k in acc..<binders[i].itemCount + acc) {
                    val v = ll.getChildAt(k)
                    if (item == v.tag && k != j + acc) {
                        ll.removeView(v)
                        ll.addView(v, j + acc)
                    }
                }
            }
            acc += binders[i].itemCount
        }
    }

    private fun rebindViews(lists: Array<out List<T>>) {
        var acc = 0

        for (i in lists.indices) {
            lists[i].forEachIndexed { j, item ->
                val v = ll.getChildAt(j + acc)
                if (v.tag != item) {
                    v.tag = item
                    binders[i].bind(item, j + acc, v)
                }
            }
            acc += binders[i].itemCount
        }
    }
}