package com.example.lupapj.app

import android.content.Context
import com.example.lupapj.data.mock.MockAuthRepository
import com.example.lupapj.data.mock.DemoScenes
import com.example.lupapj.data.mock.MockRoomRepository
import com.example.lupapj.data.model.friend.FriendCode
import com.example.lupapj.data.model.friend.FriendUser
import com.example.lupapj.data.model.scene.RoomSceneId
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.FriendRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.PlazaRepository
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
import com.example.lupapj.data.remote.ServerConfig
import com.example.lupapj.data.remote.friend.FriendRetrofitService
import com.example.lupapj.data.remote.friend.RemoteFriendRepository
import com.example.lupapj.data.remote.friend.RetrofitFriendApiClient
import com.example.lupapj.data.remote.plaza.PlazaRetrofitService
import com.example.lupapj.data.remote.plaza.RemotePlazaRepository
import com.example.lupapj.data.remote.plaza.RetrofitPlazaApiClient
import okhttp3.OkHttpClient // [추가됨(권)]
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
        .baseUrl(ServerConfig.BASE_URL) // [수정됨(권)] 개발 중인 백엔드 실제 IP 주소 (팀원 작업인 ServerConfig 유지)
        .client(okHttpClient) // OkHttpClient 연결
        .addConverterFactory(ScalarsConverterFactory.create()) // String 응답 처리용
        .addConverterFactory(GsonConverterFactory.create())    // JSON 요청 처리용
        .build()

    private val currencyApiService: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
    private val friendRetrofitService: FriendRetrofitService =
        retrofit.create(FriendRetrofitService::class.java)
    private val plazaRetrofitService: PlazaRetrofitService =
        retrofit.create(PlazaRetrofitService::class.java)
    
    // [수정됨(권)] DataSource 인스턴스화
    private val currencyRemoteDataSource = CurrencyRemoteDataSource(currencyApiService)

    val authRepository: AuthRepository = MockAuthRepository()
    private val roomLocalCache = com.example.lupapj.data.local.RoomLocalCache(appContext)
    val roomRepository: RoomRepository = MockRoomRepository(roomLocalCache)

    val friendRepository: FriendRepository = RemoteFriendRepository(
        apiClient = RetrofitFriendApiClient(friendRetrofitService),
        initialCurrentUser = FriendUser(
            userId = "unknown",
            nickname = "나",
            friendCode = FriendCode("LUPA00000")
        ),
        sceneResolver = { sceneId -> DemoScenes.sceneFor(RoomSceneId(sceneId)) }
    )
    val plazaRepository: PlazaRepository = RemotePlazaRepository(
        apiClient = RetrofitPlazaApiClient(plazaRetrofitService)
    )
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(appContext) } // [보존] 팀원 작업 내용
    
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
