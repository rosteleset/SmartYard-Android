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

interface TeledomApi {
    @POST("https://dm.lanta.me/app_static/settings/prov.json")
    suspend fun providers(): ProvidersListResponse

    @POST("/user/registerPushToken")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): Response<RegisterPushTokenResponse>

    @POST("/user/requestCode")
    suspend fun requestCode(
        @Body request: RequestCodeRequest
    ): Response<RequestCodeResponse>

    @POST("/user/confirmCode")
    suspend fun confirmCode(
        @Body request: ConfirmCodeRequest
    ): ConfirmCodeResponse

    @POST("/user/sendName")
    suspend fun sendName(
        @Body request: SendNameRequest
    ): Response<SendNameResponse>

    @POST("/geo/getServices")
    suspend fun getServices(
        @Body request: GetServicesRequest
    ): Response<GetServicesResponse>

    @POST("/geo/getAllLocations")
    suspend fun getAllLocations(): GetAllLocationsResponse

    @POST("/geo/getStreets")
    suspend fun getStreets(
        @Body request: GetStreetsRequest
    ): GetStreetsResponse

    @POST("/geo/getHouses")
    suspend fun getHouses(
        @Body request: GetHousesRequest
    ): GetHousesResponse

    @POST("/geo/address")
    suspend fun getAddress(@Body requst: GetAddressRequest): GetAddressResponse

    @POST("/geo/coder")
    suspend fun getCoder(@Body requst: GetCoderRequest): GetCoderResponse

    @POST("/address/openDoor")
    suspend fun openDoor(
        @Body request: OpenDoorRequest
    ): Response<OpenDoorResponse>

    @POST("/address/getAddressList")
    suspend fun getAddressList(): Response<GetAddressListResponse>

    @POST("/address/getSettingsList")
    suspend fun getSettingsList(): Response<GetSettingsListResponse>

    @POST("/address/intercom")
    suspend fun putIntercom(
        @Body requestPut: PutIntercomRequest
    ): IntercomResponse

    @POST("/address/intercom")
    suspend fun getIntercom(
        @Body requestPut: GetIntercomRequest
    ): IntercomResponse

    @POST("/address/resetCode")
    suspend fun resetCode(
        @Body request: ResetCodeRequest
    ): ResetCodeResponse

    @POST("/address/offices")
    suspend fun getOffices(): OfficesResponse

    @POST("/user/addMyPhone")
    suspend fun addMyPhone(@Body request: AddMyPhoneRequest): Response<AddMyPhoneResponse>

    @POST("/user/restore")
    suspend fun recoveryOptions(@Body request: RecoveryOptionsRequest): Response<RecoveryOptionsResponse>

    @POST("/user/restore")
    suspend fun sentCodeRecovery(@Body request: SentCodeRecoveryRequest): Response<SentCodeRecoveryResponse>

    @POST("/user/restore")
    suspend fun confirmCodeRecovery(@Body request: ConfirmCodeRecoveryRequest): Response<ConfirmCodeRecoveryResponse>

    @POST("/address/registerQR")
    suspend fun registerQR(@Body request: QRRequest): QRResponse

    @POST("/address/access")
    suspend fun access(@Body request: AccessRequest): Response<AccessResponse>

    @POST("/address/getSettingsList")
    suspend fun getRoommate(): RoommateResponse

    @POST("/address/resend")
    suspend fun resend(@Body request: ResendRequest): Response<ResendResponse>

    @POST("/address/plogDays")
    suspend fun plogDays(@Body request: PlogDaysRequest): Response<PlogDaysResponse>

    @POST("/address/plog")
    suspend fun plog(@Body request: PlogRequest): Response<PlogResponse>

    @POST("/inbox/inbox")
    suspend fun inbox(): InboxResponse

    @POST("/inbox/unreaded")
    suspend fun unread(): UnreadedResponse

    @POST("/inbox/delivered")
    suspend fun delivered(@Body request: DeliveredRequest): Response<DeliveredResponse>

    @POST("/issues/create")
    suspend fun createIssues(@Body request: CreateIssuesRequest): CreateIssuesResponse

    @POST("/issues/listConnect")
    suspend fun listConnectIssue(): Response<ListConnectIssueResponse>

    @POST("/issues/action")
    suspend fun actionIssue(@Body request: ActionIssueRequest): Response<ActionIssueResponse>

    @POST("/issues/comment")
    suspend fun comment(@Body request: CommentRequest): Response<CommentResponse>

    @POST("/issues/action")
    suspend fun deliveryChange(@Body request: DeliveryChangeRequest): Response<DeliveryChangeResponse>

    @POST("/cctv/all")
    suspend fun getCCTVAll(@Body request: CCTVAllRequest): Response<CCTVGetResponse>

    @POST("/cctv/recPrepare")
    suspend fun recPrepare(@Body request: CCTVRecPrepareRequest): Response<CCTVRecPrepareResponse>

    @POST("/cctv/recDownload")
    suspend fun recDownload(@Body request: CCTVRecDownloadRequest): Response<CCTVRecDownloadResponse>

    @POST("/cctv/overview")
    suspend fun getCCTVOverview(): Response<CCTVCityCameraGetResponse>

    @POST("/cctv/youtube")
    suspend fun getCCTVYoutube(@Body request: CCTVYoutubeRequest): Response<CCTVYoutubeResponse>

    @POST("/user/getPaymentsList")
    suspend fun getPaymentsList(): Response<PaymentsListResponse>

    @POST("/pay/prepare")
    suspend fun payPrepare(@Body request: PayPrepareRequest): PayPrepareResponse

    @POST("/pay/process")
    suspend fun payProcess(@Body request: PayProcessRequest): PayProcessResponse

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

    @POST("/user/appVersion")
    suspend fun appVersion(
        @Body request: AppVersionRequest
    ): Response<AppVersionResponse>

    @POST("/user/notification")
    suspend fun userNotification(
        @Body request: UserNotificationRequest
    ): UserNotificationResponse

    @GET
    suspend fun loadPeriods(@Url url: String): Response<List<RangeObject>>

    @POST("/sip/helpMe")
    suspend fun sipHelpMe(): Response<SipHelpMeResponse>

    @POST("/frs/disLike")
    suspend fun disLike(@Body request: DisLikeRequest): Response<DisLikeResponse>

    @POST("/frs/like")
    suspend fun like(@Body request: LikeRequest): Response<LikeResponse>

    @POST("/frs/listFaces")
    suspend fun listFaces(@Body request: ListFacesRequest): Response<ListFacesResponse>

    @POST("/cctv/camMap")
    suspend fun camMap(): Response<CamMapResponse>

    @POST("/ext/list")
    suspend fun extList(): Response<ExtListResponse>

    @POST("/ext/ext")
    suspend fun ext(@Body request: ExtRequest): Response<ExtResponse>
}
