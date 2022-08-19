package com.sesameware.domain.model
class example
enum class TF(var value: String) {
    TRUE("t"),
    FALSE("f");

    companion object {
        fun getBoolean(flag: Boolean): TF {
            return if (flag) TRUE else FALSE
        }

        fun getString(value: String): Boolean {
            return value == TRUE.value
        }
    }
}
