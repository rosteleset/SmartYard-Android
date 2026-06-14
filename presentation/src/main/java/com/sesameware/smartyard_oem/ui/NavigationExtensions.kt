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

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import com.sesameware.smartyard_oem.ui.main.FloatingBottomNavView
import timber.log.Timber

fun FloatingBottomNavView.setupExitOnBackPressedWhenInRoot(
    navController: NavController,
    activity: ComponentActivity
) {
    activity.onBackPressedDispatcher.addCallback(
        activity,
        object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                val menuId = selectedItemId
                val current = navController.currentDestination ?: return

                // If itemId binded to destination
                if (menuId == current.id) {
                    activity.finish()
                    return
                }

                // If itemId binded to graph
                val parent = current.parent!!
                val startId = parent.startDestinationId
                if (menuId == parent.id && startId == current.id) {
                    activity.finish()
                } else {
                    navController.navigateUp()
                }
            }
        }
    )
}

fun FloatingBottomNavView.setupPopToRootOnItemReselected(navController: NavController) {
    addOnItemReselectedListener { itemId ->
        navController.graph.findNode(itemId)?.let { rootDest ->
            val destinationIdToPop = if (rootDest is NavGraph) {
                rootDest.startDestinationId
            } else {
                rootDest.id
            }

            navController.popBackStack(destinationIdToPop, false)
        }
    }
}

fun FloatingBottomNavView.setupWithNavController(navController: NavController) {
    addOnItemSelectedListener { itemId ->
        clearBadge(itemId)

        val startDestinationId = navController.graph.findStartDestination().id

        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                destinationId = startDestinationId,
                inclusive = false,
                saveState = true
            )
            .build()

        try {
            navController.navigate(itemId, null, options)
        } catch (e: IllegalArgumentException) {
            Timber.d("debug_dmm Bottom nav item with id $itemId was not found in nav graph")
        } catch (e: Exception) {
            Timber.e(e, "debug_dmm Navigation error on item $itemId")
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        val matchedId = destination.hierarchy.firstOrNull { this.hasItem(it.id) }?.id

        if (matchedId != null && this.selectedItemId != matchedId) {
            this.setSelection(matchedId, animate = true, notify = false)
        }
    }
}