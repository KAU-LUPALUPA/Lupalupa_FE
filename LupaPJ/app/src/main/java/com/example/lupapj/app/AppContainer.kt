package com.example.lupapj.app

import android.content.Context
import com.example.lupapj.data.mock.MockAuthRepository
import com.example.lupapj.data.mock.MockRoomRepository
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.GalleryRepository // [추가됨]
import com.example.lupapj.data.repository.RoomRepository

class AppContainer(private val context: Context) { // [수정됨] Context 주입받도록 변경
    val authRepository: AuthRepository = MockAuthRepository()
    val roomRepository: RoomRepository = MockRoomRepository()
    val galleryRepository: GalleryRepository by lazy { GalleryRepository(context) } // [추가됨] 갤러리 리포지토리 생성
}
