package com.sesameware.smartyard_oem.ui.main.address.addressVerification

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabAdapter(
    private val fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val mCreateFragmentCallbackList: MutableList<() -> Fragment> = ArrayList()
    private val mFragmentTitleList: MutableList<String> = ArrayList()

    override fun createFragment(position: Int): Fragment {
        return mCreateFragmentCallbackList[position]()
    }

    fun addFragment(createFragmentCallback: () -> Fragment, title: String) {
        mCreateFragmentCallbackList.add(createFragmentCallback)
        mFragmentTitleList.add(title)
    }

    fun getPageTitle(position: Int): CharSequence {
        return mFragmentTitleList[position]
    }

    fun getItem(position: Int) = fragmentManager.findFragmentByTag("f$position")

    override fun getItemCount(): Int = mCreateFragmentCallbackList.size
}
