package com.sesameware.domain.model.response

import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 11.08.2020.
 */

data class RangeObject(
    @param:Json(name = "motion_log")
    var motionLog: List<Any> = listOf(),
    @param:Json(name = "ranges")
    var ranges: List<Range> = listOf(),
    @param:Json(name = "stream")
    var stream: String = "" // 89740
) {
    data class Range(
        @param:Json(name = "duration")
        var duration: Int = 0, // 619035
        @param:Json(name = "from")
        var from: Int = 0 // 1596517202
    )
}
