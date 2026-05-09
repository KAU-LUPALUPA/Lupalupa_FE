package com.example.lupapj.app

import android.content.Context
import com.example.lupapj.data.mock.MockAuthRepository
import com.example.lupapj.data.mock.MockFriendRepository
import com.example.lupapj.data.mock.MockRoomRepository
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.mock.MockCurrencyRepository
import com.example.lupapj.data.mock.MockShopRepository
import com.example.lupapj.data.repository.CurrencyRepository
import com.example.lupapj.data.repository.ShopRepository
import com.example.lupapj.data.local.ShopLocalCache // [추가됨(권)] 로컬 캐시 임포트

import com.example.lupapj.data.repository.NetworkCurrencyRepository // [추가됨(권)]
import com.example.lupapj.data.remote.CurrencyApiService // [추가됨(권)]
import com.example.lupapj.data.remote.AuthInterceptor // [추가됨(권)]
import com.example.lupapj.data.remote.CurrencyRemoteDataSource // [추가됨(권)]
import okhttp3.OkHttpClient // [추가됨(권)]
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory // 문자열 응답 처리를 위해 필요

class AppContainer(context: Context) { // [수정됨] Context 주입받도록 변경
    private val appContext = context.applicationContext

    // [추가됨(권)] OkHttpClient 및 AuthInterceptor 설정
    private val authInterceptor = AuthInterceptor {
        // AppViewModel에서 저장한 실제 카카오 로그인 토큰을 가져옵니다.
        com.example.lupapj.data.local.TokenManager.accessToken
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // [추가됨(권)] Retrofit 설정 (기본 URL 및 컨버터 설정)
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://54.180.145.58:8080/") // [수정됨(권)] 개발 중인 백엔드 실제 IP 주소
        .client(okHttpClient) // OkHttpClient 연결
        .addConverterFactory(ScalarsConverterFactory.create()) // String 응답 처리용
        .addConverterFactory(GsonConverterFactory.create())    // JSON 요청 처리용
        .build()

    private val currencyApiService: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
    
    // [추가됨(권)] DataSource 인스턴스화
    private val currencyRemoteDataSource = CurrencyRemoteDataSource(currencyApiService)

    val authRepository: AuthRepository = MockAuthRepository()
    val roomRepository: RoomRepository = MockRoomRepository()
    val friendRepository: FriendRepository = MockFriendRepository()
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(appContext) } // [추가됨] 갤러리 리포지토리 생성
    
    // [추가됨(권)] 로컬 캐시 인스턴스. 서버 응답 성공 후에만 기록되는 읽기 캐시 역할.
    private val shopLocalCache = ShopLocalCache(appContext)

    // [추가됨(권)] 개발 환경 설정을 위한 플래그
    // 서버가 준비되지 않았거나 토큰이 없을 때는 true로 설정하여 목업 데이터를 사용합니다.
    private val USE_MOCK_SERVER = false // [수정됨(권)] 실제 서버 연동 테스트를 위해 false로 변경
    
    // [수정됨(권)] 플래그에 따라 주입할 리포지토리를 동적으로 결정합니다. (Dependency Injection의 장점)
    val currencyRepository: CurrencyRepository = if (USE_MOCK_SERVER) {
        MockCurrencyRepository(shopLocalCache) // 기존 목업 동작
    } else {
        NetworkCurrencyRepository(currencyRemoteDataSource, shopLocalCache) // 실제 서버 통신
    }

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
