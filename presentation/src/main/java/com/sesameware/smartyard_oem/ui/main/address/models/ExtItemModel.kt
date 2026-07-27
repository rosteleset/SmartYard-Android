package com.sesameware.smartyard_oem.ui.main.address.models

data class ExtItemModel(
    val extId: String?,
    val caption: String?,
    val icon: String?,
    val order: Int,
    val highlight: Boolean,
    val basePath: String?,
    val code: String?,
    val version: Int,
    val isHeaderHidden: Boolean,
    val statusBarColor: String?,
    val statusBarStyle: String?,
)
