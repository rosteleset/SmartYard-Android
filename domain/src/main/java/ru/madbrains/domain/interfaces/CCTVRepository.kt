package ru.madbrains.domain.interfaces

import retrofit2.http.Url
import ru.madbrains.domain.model.response.CCTVGetResponse
import ru.madbrains.domain.model.response.CCTVRecDownloadResponse
import ru.madbrains.domain.model.response.CCTVRecPrepareResponse
import ru.madbrains.domain.model.response.CCTVYoutubeResponse
import ru.madbrains.domain.model.response.CCTVCityCameraGetResponse
import ru.madbrains.domain.model.response.RangeObject

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
interface CCTVRepository {
    suspend fun getCCTVAll(houseId: Int): CCTVGetResponse?
    suspend fun recDownload(fragmentID: Int): CCTVRecDownloadResponse?
    suspend fun recPrepare(cameraID: Int, from: String, to: String): CCTVRecPrepareResponse?
    suspend fun loadPeriods(@Url url: String): List<RangeObject>?
    suspend fun getCCTVOverview(): CCTVCityCameraGetResponse?
    suspend fun getCCTVYoutube(id: Int?): CCTVYoutubeResponse?
}
