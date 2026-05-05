package com.example.lupapj.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// [추가됨] Retrofit 인스턴스를 싱글톤으로 제공하는 팩토리 객체.
// AppContainer에서 한 번만 생성하여 재사용합니다.
object RetrofitClient {

    // [추가됨] OkHttp 클라이언트: 타임아웃 설정 및 로깅 인터셉터 포함
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // 디버그 빌드에서만 요청/응답 본문을 로그에 출력합니다.
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // [추가됨] Retrofit 인스턴스
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // [추가됨] CurrencyApiService 싱글톤 인스턴스
    val currencyApiService: CurrencyApiService by lazy {
        retrofit.create(CurrencyApiService::class.java)
    }
}
