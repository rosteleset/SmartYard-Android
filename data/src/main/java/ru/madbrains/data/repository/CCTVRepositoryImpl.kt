package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.remote.LantaApi
import ru.madbrains.domain.interfaces.CCTVRepository
import ru.madbrains.domain.model.request.CCTVAllRequest
import ru.madbrains.domain.model.request.CCTVRecDownloadRequest
import ru.madbrains.domain.model.request.CCTVRecPrepareRequest
import ru.madbrains.domain.model.request.CCTVSortRequest
import ru.madbrains.domain.model.request.CCTVYoutubeRequest
import ru.madbrains.domain.model.response.CCTVGetResponse
import ru.madbrains.domain.model.response.CCTVRecDownloadResponse
import ru.madbrains.domain.model.response.CCTVRecPrepareResponse
import ru.madbrains.domain.model.response.CCTVCityCameraGetResponse
import ru.madbrains.domain.model.response.CCTVSortResponse
import ru.madbrains.domain.model.response.RangeObject
import ru.madbrains.domain.model.response.CCTVYoutubeResponse

/**
 * @author Nail Shakurov
 * Created on 01/04/2020.
 */
class CCTVRepositoryImpl(
    private val lantaApi: LantaApi,
    override val moshi: Moshi
) : CCTVRepository, BaseRepository(moshi) {
    override suspend fun getCCTVAll(houseId: Int): CCTVGetResponse? {
        return safeApiCall {
            lantaApi.getCCTVAll(CCTVAllRequest(houseId)).getResponseBody()
        }
    }
    override suspend fun recDownload(fragmentID: Int): CCTVRecDownloadResponse? {
        return safeApiCall {
            lantaApi.recDownload(CCTVRecDownloadRequest(fragmentID)).getResponseBody()
        }
    }

    override suspend fun recPrepare(
        cameraID: Int,
        from: String,
        to: String
    ): CCTVRecPrepareResponse? {
        return safeApiCall {
            lantaApi.recPrepare(CCTVRecPrepareRequest(cameraID, from, to)).getResponseBody()
        }
    }

    override suspend fun loadPeriods(url: String): List<RangeObject>? {
        return safeApiCall {
            lantaApi.loadPeriods(url).getResponseBody()
        }
    }

    override suspend fun getCCTVOverview(): CCTVCityCameraGetResponse? {
        return safeApiCall {
            lantaApi.getCCTVOverview().getResponseBody()
        }
    }

    override suspend fun setCCTVSort(list: List<Int>): CCTVSortResponse {
        return safeApiCall {
            lantaApi.setCCTVSort(CCTVSortRequest(list)).getResponseBody()
        }
    }

    override suspend fun getCCTVYoutube(id: Int?): CCTVYoutubeResponse? {
        return safeApiCall {
            lantaApi.getCCTVYoutube(CCTVYoutubeRequest(id)).getResponseBody()
        }
    }
}
