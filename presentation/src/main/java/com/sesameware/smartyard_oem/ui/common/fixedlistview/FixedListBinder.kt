package com.sesameware.smartyard_oem.ui.common.fixedlistview

import android.view.View
import android.view.ViewGroup

abstract class FixedListBinder<T : FixedListItem>(val itemType: Class<T>) {

    // Do not attach to parent
    abstract fun inflate(parent: ViewGroup): View

    abstract fun bind(view: View, item: T, animate: Boolean = false)
}