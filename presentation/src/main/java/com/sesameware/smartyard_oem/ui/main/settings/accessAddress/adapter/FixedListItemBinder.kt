package com.sesameware.smartyard_oem.ui.main.settings.accessAddress.adapter

import android.view.View
import android.view.ViewGroup

interface FixedListItemBinder<T> {
    val itemCount: Int
    fun inflate(parent: ViewGroup)
    fun bind(item: T, position: Int, rootView: View)
}