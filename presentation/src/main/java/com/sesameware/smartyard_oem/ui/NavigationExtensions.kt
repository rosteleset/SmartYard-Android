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
import androidx.navigation.NavGraph
import com.google.android.material.bottomnavigation.BottomNavigationView

fun BottomNavigationView.setupExitOnBackPressedWhenInRoot(
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

fun BottomNavigationView.setupPopToRootOnItemReselected(
    navController: NavController,
) {
    setOnItemReselectedListener { item ->
        val rootDestination = navController.graph
            .findNode(item.itemId) ?: return@setOnItemReselectedListener

        val destinationIdToPop = if (rootDestination is NavGraph) {
            rootDestination.startDestinationId
        } else {
            rootDestination.id
        }

        navController.popBackStack(destinationIdToPop, false)
    }
}