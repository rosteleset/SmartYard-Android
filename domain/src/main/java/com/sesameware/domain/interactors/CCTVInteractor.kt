package com.sesameware.domain.interactors

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.http.Url
import com.sesameware.domain.interfaces.CCTVRepository
import com.sesameware.domain.model.response.CCTVCityCameraData
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.domain.model.response.CCTVYoutubeData
import com.sesameware.domain.model.response.RangeObject

/**
 * @author Nail Shakurov
 * Created on 11/03/2020.
 */
class CCTVInteractor(
    private val repository: CCTVRepository
) {
    private val mDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun getCCTV(houseId: Int): List<CCTVData>? {
        return repository.getCCTVAll(houseId)?.data
    }

    suspend fun recDownload(fragmentID: Int): String? {
        return repository.recDownload(fragmentID)?.data
    }

    suspend fun recPrepare(
        cameraID: Int,
        from: LocalDateTime,
        to: LocalDateTime
    ): Int? {
        return repository.recPrepare(
            cameraID,
            from.format(mDateFormatter),
            to.format(mDateFormatter)
        )?.data
    }

    suspend fun loadPeriods(@Url url: String): List<RangeObject>? {
        return repository.loadPeriods(url)
    }

    suspend fun getCCTVOverview(): List<CCTVCityCameraData>? {
        return repository.getCCTVOverview()?.data
    }

    suspend fun getCCTVYoutube(id: Int?): List<CCTVYoutubeData>? {
        return repository.getCCTVYoutube(id)?.data
    }

    suspend fun ranges(cameraID: Int): List<RangeObject>? {
        return repository.ranges(cameraID)?.data
    }

    suspend fun getRealHlsUrlMacroscop(@Url url: String): String {
        return repository.getRealHlsUrlMacroscop(url)
    }
}
