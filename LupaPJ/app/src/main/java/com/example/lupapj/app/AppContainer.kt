package com.example.lupapj.app

import com.example.lupapj.data.mock.MockAuthRepository
import com.example.lupapj.data.mock.MockRoomRepository
import com.example.lupapj.data.repository.AuthRepository
import com.example.lupapj.data.repository.RoomRepository

class AppContainer {
    val authRepository: AuthRepository = MockAuthRepository()
    val roomRepository: RoomRepository = MockRoomRepository()
}
