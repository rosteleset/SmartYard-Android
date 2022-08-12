package ru.madbrains.data.repository

import com.squareup.moshi.Moshi
import ru.madbrains.data.DataModule
import ru.madbrains.data.remote.TeledomApi
import ru.madbrains.domain.interfaces.CCTVRepository
import ru.madbrains.domain.model.request.CCTVAllRequest
import ru.madbrains.domain.model.request.CCTVRecDownloadRequest
import ru.madbrains.domain.model.request.CCTVRecPrepareRequest
import ru.madbrains.domain.model.request.CCTVYoutubeRequest
import ru.madbrains.domain.model.response.CCTVGetResponse
import ru.madbrains.domain.model.response.CCTVRecDownloadResponse
import ru.madbrains.domain.model.response.CCTVRecPrepareResponse
import ru.madbrains.domain.model.response.CCTVCityCameraGetResponse
import ru.madbrains.domain.model.response.RangeObject
import ru.madbrains.domain.model.response.CCTVYoutubeResponse

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
}
