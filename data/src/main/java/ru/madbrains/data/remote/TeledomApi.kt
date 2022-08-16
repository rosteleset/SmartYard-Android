package ru.madbrains.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import ru.madbrains.domain.model.request.AccessRequest
import ru.madbrains.domain.model.request.ActionIssueRequest
import ru.madbrains.domain.model.request.AddMyPhoneRequest
import ru.madbrains.domain.model.request.AppVersionRequest
import ru.madbrains.domain.model.request.CCTVAllRequest
import ru.madbrains.domain.model.request.CCTVRecDownloadRequest
import ru.madbrains.domain.model.request.CCTVRecPrepareRequest
import ru.madbrains.domain.model.request.CCTVYoutubeRequest
import ru.madbrains.domain.model.request.ConfirmCodeRecoveryRequest
import ru.madbrains.domain.model.request.ConfirmCodeRequest
import ru.madbrains.domain.model.request.CreateIssuesRequest
import ru.madbrains.domain.model.request.DeliveredRequest
import ru.madbrains.domain.model.request.DeliveryChangeRequest
import ru.madbrains.domain.model.request.GetAddressRequest
import ru.madbrains.domain.model.request.GetCoderRequest
import ru.madbrains.domain.model.request.GetHousesRequest
import ru.madbrains.domain.model.request.GetIntercomRequest
import ru.madbrains.domain.model.request.GetServicesRequest
import ru.madbrains.domain.model.request.GetStreetsRequest
import ru.madbrains.domain.model.request.OpenDoorRequest
import ru.madbrains.domain.model.request.PayPrepareRequest
import ru.madbrains.domain.model.request.PayProcessRequest
import ru.madbrains.domain.model.request.PaymentDoRequest
import ru.madbrains.domain.model.request.PutIntercomRequest
import ru.madbrains.domain.model.request.QRRequest
import ru.madbrains.domain.model.request.RecoveryOptionsRequest
import ru.madbrains.domain.model.request.RegisterPushTokenRequest
import ru.madbrains.domain.model.request.RequestCodeRequest
import ru.madbrains.domain.model.request.ResendRequest
import ru.madbrains.domain.model.request.ResetCodeRequest
import ru.madbrains.domain.model.request.SendNameRequest
import ru.madbrains.domain.model.request.SentCodeRecoveryRequest
import ru.madbrains.domain.model.request.UserNotificationRequest
import ru.madbrains.domain.model.request.CommentRequest
import ru.madbrains.domain.model.request.PlogDaysRequest
import ru.madbrains.domain.model.request.PlogRequest
import ru.madbrains.domain.model.request.DisLikeRequest
import ru.madbrains.domain.model.request.LikeRequest
import ru.madbrains.domain.model.request.ListFacesRequest
import ru.madbrains.domain.model.request.ExtRequest
import ru.madbrains.domain.model.response.AccessResponse
import ru.madbrains.domain.model.response.ActionIssueResponse
import ru.madbrains.domain.model.response.AddMyPhoneResponse
import ru.madbrains.domain.model.response.AppVersionResponse
import ru.madbrains.domain.model.response.CCTVGetResponse
import ru.madbrains.domain.model.response.CCTVRecDownloadResponse
import ru.madbrains.domain.model.response.CCTVRecPrepareResponse
import ru.madbrains.domain.model.response.ConfirmCodeRecoveryResponse
import ru.madbrains.domain.model.response.ConfirmCodeResponse
import ru.madbrains.domain.model.response.CreateIssuesResponse
import ru.madbrains.domain.model.response.DeliveredResponse
import ru.madbrains.domain.model.response.DeliveryChangeResponse
import ru.madbrains.domain.model.response.GetAddressListResponse
import ru.madbrains.domain.model.response.GetAddressResponse
import ru.madbrains.domain.model.response.GetAllLocationsResponse
import ru.madbrains.domain.model.response.GetCoderResponse
import ru.madbrains.domain.model.response.GetHousesResponse
import ru.madbrains.domain.model.response.GetServicesResponse
import ru.madbrains.domain.model.response.GetSettingsListResponse
import ru.madbrains.domain.model.response.GetStreetsResponse
import ru.madbrains.domain.model.response.InboxResponse
import ru.madbrains.domain.model.response.IntercomResponse
import ru.madbrains.domain.model.response.ListConnectIssueResponse
import ru.madbrains.domain.model.response.OfficesResponse
import ru.madbrains.domain.model.response.OpenDoorResponse
import ru.madbrains.domain.model.response.PayPrepareResponse
import ru.madbrains.domain.model.response.PayProcessResponse
import ru.madbrains.domain.model.response.PaymentDoResponse
import ru.madbrains.domain.model.response.PaymentsListResponse
import ru.madbrains.domain.model.response.QRResponse
import ru.madbrains.domain.model.response.RangeObject
import ru.madbrains.domain.model.response.RecoveryOptionsResponse
import ru.madbrains.domain.model.response.RegisterPushTokenResponse
import ru.madbrains.domain.model.response.RequestCodeResponse
import ru.madbrains.domain.model.response.SendNameResponse
import ru.madbrains.domain.model.response.ResendResponse
import ru.madbrains.domain.model.response.ResetCodeResponse
import ru.madbrains.domain.model.response.RoommateResponse
import ru.madbrains.domain.model.response.SentCodeRecoveryResponse
import ru.madbrains.domain.model.response.UnreadedResponse
import ru.madbrains.domain.model.response.UserNotificationResponse
import ru.madbrains.domain.model.response.CommentResponse
import ru.madbrains.domain.model.response.CCTVCityCameraGetResponse
import ru.madbrains.domain.model.response.CCTVYoutubeResponse
import ru.madbrains.domain.model.response.SipHelpMeResponse
import ru.madbrains.domain.model.response.PlogDaysResponse
import ru.madbrains.domain.model.response.PlogResponse
import ru.madbrains.domain.model.response.DisLikeResponse
import ru.madbrains.domain.model.response.LikeResponse
import ru.madbrains.domain.model.response.ListFacesResponse
import ru.madbrains.domain.model.response.CamMapResponse
import ru.madbrains.domain.model.response.SberRegisterDoReponse
import ru.madbrains.domain.model.response.SberOrderStatusDoResponse
import ru.madbrains.domain.model.response.ExtListResponse
import ru.madbrains.domain.model.response.ExtResponse
import ru.madbrains.domain.model.response.ProvidersListResponse
import ru.madbrains.domain.model.response.ProviderConfigResponse
import ru.madbrains.domain.model.response.AuthTypesResponse

interface TeledomApi {
    @GET("https://192.168.13.39:9091/prov.json")
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
    suspend fun delivered(
        @Url url: String,
        @Body request: DeliveredRequest): Response<DeliveredResponse>

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
    suspend fun authTypes(@Url url: String): AuthTypesResponse
}
