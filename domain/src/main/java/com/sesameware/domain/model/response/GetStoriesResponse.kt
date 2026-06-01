package com.sesameware.domain.model.response

import com.squareup.moshi.Json

typealias GetStoriesResponse = ApiResult<List<Story>>?

data class Story(
    @param:Json(name = "imageUrl") val imageUrl: String,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "subtitle") val subtitle: String,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "presentMethod") val presentMethod: String
)

const val PRESENT_METHOD_VIEW = "view"
const val PRESENT_METHOD_POPUP = "popup"
const val PRESENT_METHOD_OPEN_APP = "openApp"
