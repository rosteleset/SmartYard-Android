package com.sesameware.data.remote

import com.sesameware.data.BuildConfig
import com.sesameware.domain.model.request.AccessRequest
import com.sesameware.domain.model.request.ActionIssueRequest
import com.sesameware.domain.model.request.ActionIssueRequestV2
import com.sesameware.domain.model.request.AddLicensePlateNumberRequest
import com.sesameware.domain.model.request.AddMyPhoneRequest
import com.sesameware.domain.model.request.AppVersionRequest
import com.sesameware.domain.model.request.CCTVAllRequest
import com.sesameware.domain.model.request.CCTVRangesRequest
import com.sesameware.domain.model.request.CCTVRecDownloadRequest
import com.sesameware.domain.model.request.CCTVRecPrepareRequest
import com.sesameware.domain.model.request.CCTVYoutubeRequest
import com.sesameware.domain.model.request.CommentRequest
import com.sesameware.domain.model.request.ConfirmCodeRecoveryRequest
import com.sesameware.domain.model.request.ConfirmCodeRequest
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.request.CreateIssuesRequestV2
import com.sesameware.domain.model.request.DeliveryChangeRequest
import com.sesameware.domain.model.request.DisLikeRequest
import com.sesameware.domain.model.request.ExtRequest
import com.sesameware.domain.model.request.GetAddressRequest
import com.sesameware.domain.model.request.GetCoderRequest
import com.sesameware.domain.model.request.GetHousesRequest
import com.sesameware.domain.model.request.GetIntercomRequest
import com.sesameware.domain.model.request.GetServicesRequest
import com.sesameware.domain.model.request.GetStreetsRequest
import com.sesameware.domain.model.request.LikeRequest
import com.sesameware.domain.model.request.ListFacesRequest
import com.sesameware.domain.model.request.ListLicensePlateNumbersRequest
import com.sesameware.domain.model.request.OpenDoorRequest
import com.sesameware.domain.model.request.PayPrepareRequest
import com.sesameware.domain.model.request.PayRegisterRequest
import com.sesameware.domain.model.request.PlogDaysRequest
import com.sesameware.domain.model.request.PlogRequest
import com.sesameware.domain.model.request.PutIntercomRequest
import com.sesameware.domain.model.request.QRRequest
import com.sesameware.domain.model.request.RecoveryOptionsRequest
import com.sesameware.domain.model.request.RegisterPushTokenRequest
import com.sesameware.domain.model.request.RemoveLicensePlateNumberRequest
import com.sesameware.domain.model.request.RequestCodeRequest
import com.sesameware.domain.model.request.ResendRequest
import com.sesameware.domain.model.request.ResetCodeRequest
import com.sesameware.domain.model.request.SendNameRequest
import com.sesameware.domain.model.request.SentCodeRecoveryRequest
import com.sesameware.domain.model.request.UserNotificationRequest
import com.sesameware.domain.model.response.AccessResponse
import com.sesameware.domain.model.response.ActionIssueResponse
import com.sesameware.domain.model.response.AddLicensePlateNumberResponse
import com.sesameware.domain.model.response.AddMyPhoneResponse
import com.sesameware.domain.model.response.ApiResult
import com.sesameware.domain.model.response.AppVersionResponse
import com.sesameware.domain.model.response.CCTVCityCameraGetResponse
import com.sesameware.domain.model.response.CCTVGetResponse
import com.sesameware.domain.model.response.CCTVRangesResponse
import com.sesameware.domain.model.response.CCTVRecDownloadResponse
import com.sesameware.domain.model.response.CCTVRecPrepareResponse
import com.sesameware.domain.model.response.CCTVTreeResponse
import com.sesameware.domain.model.response.CCTVYoutubeResponse
import com.sesameware.domain.model.response.CamMapResponse
import com.sesameware.domain.model.response.CommentResponse
import com.sesameware.domain.model.response.ConfirmCodeRecoveryResponse
import com.sesameware.domain.model.response.ConfirmCodeResponse
import com.sesameware.domain.model.response.CreateIssuesResponse
import com.sesameware.domain.model.response.DeliveryChangeResponse
import com.sesameware.domain.model.response.DisLikeResponse
import com.sesameware.domain.model.response.ExtListResponse
import com.sesameware.domain.model.response.ExtResponse
import com.sesameware.domain.model.response.GetAddressListResponse
import com.sesameware.domain.model.response.GetAddressResponse
import com.sesameware.domain.model.response.GetAllLocationsResponse
import com.sesameware.domain.model.response.GetCoderResponse
import com.sesameware.domain.model.response.GetHousesResponse
import com.sesameware.domain.model.response.GetServicesResponse
import com.sesameware.domain.model.response.GetSettingsListResponse
import com.sesameware.domain.model.response.GetStreetsResponse
import com.sesameware.domain.model.response.InboxResponse
import com.sesameware.domain.model.response.IntercomResponse
import com.sesameware.domain.model.response.LikeResponse
import com.sesameware.domain.model.response.ListConnectIssueResponse
import com.sesameware.domain.model.response.ListFacesResponse
import com.sesameware.domain.model.response.ListLicensePlatesNumbersResponse
import com.sesameware.domain.model.response.OfficesResponse
import com.sesameware.domain.model.response.OpenDoorResponse
import com.sesameware.domain.model.response.PayPrepareResponse
import com.sesameware.domain.model.response.PayRegisterResponse
import com.sesameware.domain.model.response.PaymentsListResponse
import com.sesameware.domain.model.response.PlogDaysResponse
import com.sesameware.domain.model.response.PlogResponse
import com.sesameware.domain.model.response.ProviderConfigResponse
import com.sesameware.domain.model.response.ProvidersListResponse
import com.sesameware.domain.model.response.QRResponse
import com.sesameware.domain.model.response.RangeObject
import com.sesameware.domain.model.response.RecoveryOptionsResponse
import com.sesameware.domain.model.response.RegisterPushTokenResponse
import com.sesameware.domain.model.response.RemoveLicensePlateResponse
import com.sesameware.domain.model.response.RequestCodeResponse
import com.sesameware.domain.model.response.ResendResponse
import com.sesameware.domain.model.response.ResetCodeResponse
import com.sesameware.domain.model.response.RoommateResponse
import com.sesameware.domain.model.response.SendNameResponse
import com.sesameware.domain.model.response.SentCodeRecoveryResponse
import com.sesameware.domain.model.response.SipHelpMeResponse
import com.sesameware.domain.model.response.UnreadedResponse
import com.sesameware.domain.model.response.UserNotificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TeledomApi {
    @GET(BuildConfig.PROVIDERS_URL)
    suspend fun providers(): ProvidersListResponse

    @POST
    suspend fun registerPushToken(
        @Url url: String,
        @Body request: RegisterPushTokenRequest
    ): Response<RegisterPushTokenResponse>

    @POST
    suspend fun requestCode(
        @Url ulr: String,
        @Body request: RequestCodeRequest
    ): Response<RequestCodeResponse>

    @POST
    suspend fun confirmCode(
        @Url url: String,
        @Body request: ConfirmCodeRequest
    ): ConfirmCodeResponse

    @POST
    suspend fun checkPhone(
        @Url url: String,
        @Body request: RequestCodeRequest
    ): ConfirmCodeResponse

    @POST
    suspend fun sendName(
        @Url url: String,
        @Body request: SendNameRequest
    ): Response<SendNameResponse>

    @POST
    suspend fun getServices(
        @Url url: String,
        @Body request: GetServicesRequest
    ): Response<GetServicesResponse>

    @POST
    suspend fun getAllLocations(@Url url: String): GetAllLocationsResponse

    @POST
    suspend fun getStreets(
        @Url url: String,
        @Body request: GetStreetsRequest
    ): GetStreetsResponse

    @POST
    suspend fun getHouses(
        @Url url: String,
        @Body request: GetHousesRequest
    ): GetHousesResponse

    @POST
    suspend fun getAddress(
        @Url url: String,
        @Body request: GetAddressRequest): GetAddressResponse

    @POST
    suspend fun getCoder(
        @Url url: String,
        @Body request: GetCoderRequest): GetCoderResponse

    @POST
    suspend fun openDoor(
        @Url url: String,
        @Body request: OpenDoorRequest
    ): Response<OpenDoorResponse>

    @POST
    suspend fun getAddressList(@Url url: String): Response<GetAddressListResponse>

    @POST
    suspend fun getSettingsList(@Url url: String): Response<GetSettingsListResponse>

    @POST
    suspend fun putIntercom(
        @Url url: String,
        @Body requestPut: PutIntercomRequest
    ): IntercomResponse

    @POST
    suspend fun getIntercom(
        @Url url: String,
        @Body requestPut: GetIntercomRequest
    ): IntercomResponse

    @POST
    suspend fun resetCode(
        @Url url: String,
        @Body request: ResetCodeRequest
    ): ResetCodeResponse

    @POST
    suspend fun getOffices(@Url url:String): OfficesResponse

    @POST
    suspend fun addMyPhone(
        @Url url: String,
        @Body request: AddMyPhoneRequest): Response<AddMyPhoneResponse>

    @POST
    suspend fun recoveryOptions(
        @Url url: String,
        @Body request: RecoveryOptionsRequest): Response<RecoveryOptionsResponse>

    @POST
    suspend fun sentCodeRecovery(
        @Url url: String,
        @Body request: SentCodeRecoveryRequest): Response<SentCodeRecoveryResponse>

    @POST
    suspend fun confirmCodeRecovery(
        @Url url: String,
        @Body request: ConfirmCodeRecoveryRequest): Response<ConfirmCodeRecoveryResponse>

    @POST
    suspend fun registerQR(
        @Url url: String,
        @Body request: QRRequest): QRResponse

    @POST
    suspend fun access(
        @Url url: String,
        @Body request: AccessRequest): Response<AccessResponse>

    @POST
    suspend fun getRoommate(@Url url: String): RoommateResponse

    @POST
    suspend fun resend(
        @Url url: String,
        @Body request: ResendRequest): Response<ResendResponse>

    @POST
    suspend fun plogDays(
        @Url url: String,
        @Body request: PlogDaysRequest): Response<PlogDaysResponse>

    @POST
    suspend fun plog(
        @Url url: String,
        @Body request: PlogRequest): Response<PlogResponse>

    @POST
    suspend fun inbox(@Url url: String): InboxResponse

    @POST
    suspend fun unread(@Url url: String): UnreadedResponse

    @POST
    suspend fun createIssues(
        @Url url: String,
        @Body request: CreateIssuesRequest): CreateIssuesResponse

    @POST
    suspend fun createIssueV2(
        @Url url: String,
        @Body request: CreateIssuesRequestV2): CreateIssuesResponse

    @POST
    suspend fun listConnectIssue(@Url url: String): Response<ListConnectIssueResponse>

    @POST
    suspend fun listConnectIssueV2(@Url url: String): Response<ListConnectIssueResponse>

    @POST
    suspend fun actionIssue(
        @Url url: String,
        @Body request: ActionIssueRequest): Response<ActionIssueResponse>

    @POST
    suspend fun actionIssueV2(
        @Url url: String,
        @Body request: ActionIssueRequestV2): Response<ActionIssueResponse>

    @POST
    suspend fun comment(
        @Url url: String,
        @Body request: CommentRequest): Response<CommentResponse>

    @POST
    suspend fun commentV2(
        @Url url: String,
        @Body request: CommentRequest): Response<CommentResponse>

    @POST
    suspend fun deliveryChange(
        @Url url: String,
        @Body request: DeliveryChangeRequest): Response<DeliveryChangeResponse>

    @POST
    suspend fun getCCTVAll(
        @Url url: String,
        @Body request: CCTVAllRequest): Response<CCTVGetResponse>

    @POST
    suspend fun getCCTVAllTree(
        @Url url: String,
        @Body request: CCTVAllRequest): Response<CCTVTreeResponse>

    @POST
    suspend fun recPrepare(
        @Url url: String,
        @Body request: CCTVRecPrepareRequest): Response<CCTVRecPrepareResponse>

    @POST
    suspend fun recDownload(
        @Url url: String,
        @Body request: CCTVRecDownloadRequest): Response<CCTVRecDownloadResponse>

    @POST
    suspend fun getCCTVOverview(@Url url: String): Response<CCTVCityCameraGetResponse>

    @POST
    suspend fun getCCTVYoutube(
        @Url url: String,
        @Body request: CCTVYoutubeRequest): Response<CCTVYoutubeResponse>

    @POST
    suspend fun getPaymentsList(@Url url: String): Response<PaymentsListResponse>

    @POST
    suspend fun payPrepare(
        @Url url: String,
        @Body request: PayPrepareRequest): PayPrepareResponse

    @POST
    suspend fun payRegister(
        @Url url: String,
        @Body request: PayRegisterRequest): Response<PayRegisterResponse>

    @POST
    suspend fun appVersion(
        @Url url: String,
        @Body request: AppVersionRequest
    ): Response<AppVersionResponse>

    @POST
    suspend fun userNotification(
        @Url url: String,
        @Body request: UserNotificationRequest
    ): UserNotificationResponse

    @GET
    suspend fun loadPeriods(@Url url: String): Response<List<RangeObject>>

    @POST
    suspend fun ranges(
        @Url url: String,
        @Body request: CCTVRangesRequest): Response<CCTVRangesResponse>

    @POST
    suspend fun sipHelpMe(@Url url: String): Response<SipHelpMeResponse>

    @POST
    suspend fun disLike(
        @Url url: String,
        @Body request: DisLikeRequest): Response<DisLikeResponse>

    @POST
    suspend fun like(
        @Url url: String,
        @Body request: LikeRequest): Response<LikeResponse>

    @POST
    suspend fun listFaces(
        @Url url: String,
        @Body request: ListFacesRequest): Response<ListFacesResponse>

    @POST
    suspend fun addNumber(
        @Url url: String,
        @Body request: AddLicensePlateNumberRequest
    ): Response<AddLicensePlateNumberResponse>

    @POST
    suspend fun listNumbers(
        @Url url: String,
        @Body request: ListLicensePlateNumbersRequest
    ): Response<ListLicensePlatesNumbersResponse>

    @POST
    suspend fun removeNumber(
        @Url url: String,
        @Body request: RemoveLicensePlateNumberRequest
    ): Response<RemoveLicensePlateResponse>

    @POST
    suspend fun camMap(@Url url: String): Response<CamMapResponse>

    @POST
    suspend fun extList(@Url url: String): Response<ExtListResponse>

    @POST
    suspend fun ext(
        @Url url: String,
        @Body request: ExtRequest): Response<ExtResponse>

    @POST
    suspend fun getOptions(@Url url: String): Response<ProviderConfigResponse>

    @POST
    suspend fun phonePattern(@Url url: String): ApiResult<String>?
}
