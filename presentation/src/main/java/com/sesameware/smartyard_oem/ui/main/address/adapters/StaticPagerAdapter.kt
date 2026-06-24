package com.sesameware.smartyard_oem.ui.main.address.adapters

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class StaticPagerAdapter(private val views: List<View>) : PagerAdapter() {

    override fun getCount(): Int = views.size

    override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = views[position]
        (view.parent as? ViewGroup)?.removeView(view)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }
}