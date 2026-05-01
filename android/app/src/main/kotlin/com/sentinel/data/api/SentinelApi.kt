package com.sentinel.data.api

import com.sentinel.data.api.models.*
import retrofit2.http.*

interface SentinelApi {

    @GET("api/safe-zone")
    suspend fun getSafeZone(): SafeZoneDto

    @PUT("api/safe-zone")
    suspend fun putSafeZone(@Body zone: SafeZoneDto): SafeZoneDto

    @GET("api/events")
    suspend fun getEvents(
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null,
    ): List<EventDto>

    @DELETE("api/events")
    suspend fun clearEvents()

    @GET("api/settings")
    suspend fun getSettings(): SettingsDto

    @PUT("api/settings")
    suspend fun putSettings(@Body settings: SettingsDto): SettingsDto

    @POST("api/devices/register")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): RegisterDeviceResponse

    @POST("api/webrtc/offer")
    suspend fun postOffer(@Body offer: WebRtcOfferRequest): WebRtcAnswerResponse
}
