package ru.madbrains.domain.model.request
import com.squareup.moshi.Json

/**
 * @author Nail Shakurov
 * Created on 20/03/2020.
 */
data class RecoveryOptionsRequest(
    @Json(name = "contract")
    val contract: String = "" // f70392
)
