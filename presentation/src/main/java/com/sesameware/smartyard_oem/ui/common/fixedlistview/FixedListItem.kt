package com.sesameware.smartyard_oem.ui.common.fixedlistview

interface FixedListItem {
    fun distinctiveFieldEquals(other: FixedListItem): Boolean
}