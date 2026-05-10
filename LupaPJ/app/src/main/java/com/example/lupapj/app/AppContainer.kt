package com.example.lupapj.app

import android.content.Context
import com.example.lupapj.data.mock.MockAuthRepository
import com.example.lupapj.data.mock.MockFriendRepository
import com.example.lupapj.data.mock.MockRoomRepository
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.mock.MockCurrencyRepository
import com.example.lupapj.data.mock.MockShopRepository
import com.example.lupapj.data.repository.CurrencyRepository
import com.example.lupapj.data.repository.ShopRepository
import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.repository.NetworkCurrencyRepository
import com.example.lupapj.data.remote.CurrencyApiService
import com.example.lupapj.data.remote.AuthInterceptor
import com.example.lupapj.data.remote.CurrencyRemoteDataSource
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // [수정됨(권)] OkHttpClient 및 AuthInterceptor 설정
    private val authInterceptor = AuthInterceptor {
        com.example.lupapj.data.local.TokenManager.accessToken
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // [수정됨(권)] Retrofit 설정 (기본 URL 및 컨버터 설정)
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://15.164.49.236:8080/") // [수정됨(권)] 새로운 백엔드 실제 IP 주소로 업데이트
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val currencyApiService: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
    
    // [수정됨(권)] DataSource 인스턴스화
    private val currencyRemoteDataSource = CurrencyRemoteDataSource(currencyApiService)

    val authRepository: AuthRepository = MockAuthRepository()
    val roomRepository: RoomRepository = MockRoomRepository()
    val friendRepository: FriendRepository = MockFriendRepository()
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(appContext) }
    
    // [수정됨(권)] 로컬 캐시 인스턴스
    private val shopLocalCache = ShopLocalCache(appContext)

    // [수정됨(권)] 개발 환경 설정을 위한 플래그 (false면 실제 서버 연동)
    private val USE_MOCK_SERVER = false 
    
    // [수정됨(권)] 플래그에 따라 재화 리포지토리를 동적으로 결정
    val currencyRepository: CurrencyRepository = if (USE_MOCK_SERVER) {
        MockCurrencyRepository(shopLocalCache)
    } else {
        NetworkCurrencyRepository(currencyRemoteDataSource, shopLocalCache)
    }

    // [수정됨(권)] 플래그에 따라 상점 리포지토리를 동적으로 결정
    val shopRepository: ShopRepository = if (USE_MOCK_SERVER) {
        MockShopRepository(currencyRepository, shopLocalCache)
    } else {
        val shopApiService = retrofit.create(com.example.lupapj.data.remote.ShopApiService::class.java)
        com.example.lupapj.data.repository.RemoteShopRepository(
            apiService = shopApiService,
            currencyRepository = currencyRepository,
            localCache = shopLocalCache
        )
    }
}
