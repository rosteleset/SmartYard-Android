/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sesameware.smartyard_oem.ui

import android.content.Intent
import android.util.SparseArray
import android.view.View
import androidx.core.util.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sesameware.smartyard_oem.R

/**
 * Manages the various graphs needed for a [BottomNavigationView].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
fun BottomNavigationView.setupWithNavController(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: Intent,
    resume: Boolean
): LiveData<NavController> {
    val navHostArray = SparseArray<NavHostFragment>()
    val selectedNavController = MutableLiveData<NavController>()
    var firstFragmentGraphId = 0
    var firstFragmentGraphTag = ""
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)
        val hostFragment = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        val graphId = hostFragment.navController.graph.id
        navHostArray[graphId] = hostFragment
        if (index == 0) {
            firstFragmentGraphId = graphId
            firstFragmentGraphTag = fragmentTag
        }
        fragmentManager.beginTransaction()
            .apply {
                if (!resume && selectedItemId != graphId) {
                    detach(hostFragment)
                }
                if (selectedItemId == graphId) {
                    setPrimaryNavigationFragment(hostFragment)
                    selectedNavController.value = hostFragment.navController
                }
            }
            .commitNow()
    }
    val isOnFirstFragment = this.selectedItemId == firstFragmentGraphId
    setOnNavigationItemSelectedListener { item ->
        if (fragmentManager.isStateSaved) {
            false
        } else {
            if (this.selectedItemId != item.itemId) {
                val selectedFragment = navHostArray[item.itemId]
                fragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.nav_default_enter_anim,
                        R.anim.nav_default_exit_anim,
                        R.anim.nav_default_pop_enter_anim,
                        R.anim.nav_default_pop_exit_anim
                    )
                    .setPrimaryNavigationFragment(selectedFragment)
                    .apply {
                        navHostArray.forEach { _, host: NavHostFragment ->
                            toggleTab(host, select = host == selectedFragment)
                        }
                    }
                    .setReorderingAllowed(true)
                    .commit()
                selectedNavController.value = selectedFragment.navController
                true
            } else {
                false
            }
        }
    }
    setupItemReselected(navHostArray)
    setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)
    // Finally, ensure that we update our BottomNavigationView when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentGraphTag)) {
            this.selectedItemId = firstFragmentGraphId
        }

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
    }
    return selectedNavController
}

fun FragmentTransaction.toggleTab(host: Fragment, select: Boolean): FragmentTransaction {
    val prevHidden = host.isHidden
    val hide = !select
    if (prevHidden != hide) {
        if (hide) {
            this.hide(host)
        } else {
            this.show(host)
        }
        host.childFragmentManager.fragments.forEach {
            val view = it.view
            if (hide) {
                view?.visibility = View.GONE
            } else {
                view?.visibility = View.VISIBLE
            }
            it.onHiddenChanged(hide)
        }
    }
    if (host.isDetached && select) {
        attach(host)
    }
    return this
}

private fun BottomNavigationView.setupDeepLinks(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: Intent
) {
    navGraphIds.forEachIndexed { index, navGraphId ->
        val fragmentTag = getFragmentTag(index)
        val navHostFragment = obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId
        )
        if (navHostFragment.navController.handleDeepLink(intent) && selectedItemId != navHostFragment.navController.graph.id) {
            this.selectedItemId = navHostFragment.navController.graph.id
        }
    }
}

private fun BottomNavigationView.setupItemReselected(
    navArr: SparseArray<NavHostFragment>
) {
    setOnNavigationItemReselectedListener { item ->
        val selectedFragment = navArr[item.itemId]
        val navController = selectedFragment.navController
        navController.popBackStack(
            navController.graph.startDestinationId, false
        )
    }
}

private fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }
    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(navGraphId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

fun getFragmentTag(index: Int) = "bottomNavigation#$index"
