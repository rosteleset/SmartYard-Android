package com.sesameware.domain.interfaces

import retrofit2.http.Url
import com.sesameware.domain.model.response.CCTVGetResponse
import com.sesameware.domain.model.response.CCTVRecDownloadResponse
import com.sesameware.domain.model.response.CCTVRecPrepareResponse
import com.sesameware.domain.model.response.CCTVYoutubeResponse
import com.sesameware.domain.model.response.CCTVCityCameraGetResponse
import com.sesameware.domain.model.response.RangeObject

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
