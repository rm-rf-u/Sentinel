package com.sentinel.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.store.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private class DynamicBaseUrlInterceptor(private val prefs: AppPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val baseUrl = runBlocking { prefs.baseUrl.first() }.trimEnd('/')
        if (baseUrl.isBlank()) throw IOException("Server URL not configured — complete onboarding first")
        val base = try { baseUrl.toHttpUrl() } catch (e: Exception) {
            throw IOException("Invalid server URL: $baseUrl", e)
        }
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides @Singleton
    fun provideOkHttp(prefs: AppPreferences): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .addInterceptor(DynamicBaseUrlInterceptor(prefs))
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://localhost:8000/")  // placeholder; DynamicBaseUrlInterceptor overrides per-request
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): SentinelApi =
        retrofit.create(SentinelApi::class.java)

    @Provides
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx
}
