package com.sesameware.data.remote

import com.sesameware.data.BuildConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import com.sesameware.domain.model.request.*
import com.sesameware.domain.model.response.*

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
    suspend fun listConnectIssue(@Url url: String): Response<ListConnectIssueResponse>

    @POST
    suspend fun actionIssue(
        @Url url: String,
        @Body request: ActionIssueRequest): Response<ActionIssueResponse>

    @POST
    suspend fun comment(
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
    suspend fun payProcess(
        @Url url: String,
        @Body request: PayProcessRequest): PayProcessResponse

    @POST("https://securepayments.sberbank.ru/payment/google/payment.do")
    suspend fun paymentDo(@Body request: PaymentDoRequest): Response<PaymentDoResponse>

    @FormUrlEncoded
    @POST("https://securepayments.sberbank.ru/payment/rest/register.do")
    suspend fun sberRegisterDo(
        @Field("userName") userName: String,
        @Field("password") password: String,
        @Field("language") language: String,
        @Field("returnUrl") returnUrl: String,
        @Field("failUrl") failUrl: String,
        @Field("orderNumber") orderNumber: String,
        @Field("amount") amount: Int
    ): Response<SberRegisterDoReponse>

    @FormUrlEncoded
    @POST("https://securepayments.sberbank.ru/payment/rest/getOrderStatusExtended.do")
    suspend fun sberOrderStatusDo(
        @Field("userName") userName: String,
        @Field("password") password: String,
        @Field("orderNumber") orderNumber: String
    ): Response<SberOrderStatusDoResponse>

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
    suspend fun camMap(@Url url: String): Response<CamMapResponse>

    @POST
    suspend fun extList(@Url url: String): Response<ExtListResponse>

    @POST
    suspend fun ext(
        @Url url: String,
        @Body request: ExtRequest): Response<ExtResponse>

    @POST
    suspend fun getOptions(@Url url: String): ProviderConfigResponse

    @POST
    suspend fun phonePattern(@Url url: String): ApiResult<String>?
}
