package com.sesameware.data.repository

import com.squareup.moshi.Moshi
import com.sesameware.data.DataModule
import com.sesameware.data.remote.TeledomApi
import com.sesameware.domain.interfaces.CCTVRepository
import com.sesameware.domain.model.request.CCTVAllRequest
import com.sesameware.domain.model.request.CCTVRecDownloadRequest
import com.sesameware.domain.model.request.CCTVRecPrepareRequest
import com.sesameware.domain.model.request.CCTVYoutubeRequest
import com.sesameware.domain.model.request.CCTVRangesRequest
import com.sesameware.domain.model.response.CCTVGetResponse
import com.sesameware.domain.model.response.CCTVRecDownloadResponse
import com.sesameware.domain.model.response.CCTVRecPrepareResponse
import com.sesameware.domain.model.response.CCTVCityCameraGetResponse
import com.sesameware.domain.model.response.RangeObject
import com.sesameware.domain.model.response.CCTVYoutubeResponse
import com.sesameware.domain.model.response.CCTVRangesResponse

/**
 * @author Nail Shakurov
 * Created on 01/04/2020.
 */
class CCTVRepositoryImpl(
    private val teledomApi: TeledomApi,
    override val moshi: Moshi
) : CCTVRepository, BaseRepository(moshi) {
    override suspend fun getCCTVAll(houseId: Int): CCTVGetResponse? {
        return safeApiCall {
            teledomApi.getCCTVAll(
                DataModule.BASE_URL + "cctv/all",
                CCTVAllRequest(houseId)).getResponseBody()
        }
    }
    override suspend fun recDownload(fragmentID: Int): CCTVRecDownloadResponse? {
        return safeApiCall {
            teledomApi.recDownload(
                DataModule.BASE_URL + "cctv/recDownload",
                CCTVRecDownloadRequest(fragmentID)).getResponseBody()
        }
    }

    override suspend fun recPrepare(
        cameraID: Int,
        from: String,
        to: String
    ): CCTVRecPrepareResponse? {
        return safeApiCall {
            teledomApi.recPrepare(
                DataModule.BASE_URL + "cctv/recPrepare",
                CCTVRecPrepareRequest(cameraID, from, to)).getResponseBody()
        }
    }

    override suspend fun loadPeriods(url: String): List<RangeObject>? {
        return safeApiCall {
            teledomApi.loadPeriods(url).getResponseBody()
        }
    }

    override suspend fun getCCTVOverview(): CCTVCityCameraGetResponse? {
        return safeApiCall {
            teledomApi.getCCTVOverview(DataModule.BASE_URL + "cctv/overview").getResponseBody()
        }
    }

    override suspend fun getCCTVYoutube(id: Int?): CCTVYoutubeResponse? {
        return safeApiCall {
            teledomApi.getCCTVYoutube(
                DataModule.BASE_URL + "cctv/youtube",
                CCTVYoutubeRequest(id)).getResponseBody()
        }
    }

    override suspend fun ranges(cameraID: Int): CCTVRangesResponse? {
        return safeApiCall {
            teledomApi.ranges(
                DataModule.BASE_URL + "cctv/ranges",
                CCTVRangesRequest(cameraID)).getResponseBody()
        }
    }
}
