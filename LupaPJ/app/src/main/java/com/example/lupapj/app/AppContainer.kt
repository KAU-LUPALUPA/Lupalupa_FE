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

class AppContainer(context: Context) { // [수정됨] Context 주입받도록 변경
    private val appContext = context.applicationContext

    val authRepository: AuthRepository = MockAuthRepository()
    val roomRepository: RoomRepository = MockRoomRepository()
    val friendRepository: FriendRepository = MockFriendRepository()
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(appContext) } // [추가됨] 갤러리 리포지토리 생성
    
    // [추가됨(권)] 로컬 캐시 인스턴스. 서버 응답 성공 후에만 기록되는 읽기 캐시 역할.
    private val shopLocalCache = ShopLocalCache(appContext)
    
    val currencyRepository: CurrencyRepository = MockCurrencyRepository(shopLocalCache) // [수정됨(권)] 로컬 캐시 주입
    val shopRepository: ShopRepository = MockShopRepository(currencyRepository, shopLocalCache) // [수정됨(권)] 로컬 캐시 주입
}
