package com.example.lupapj.data.network

// [추가됨] API 서버 설정을 관리하는 상수 모음.
// 백엔드 서버 URL이 변경되면 BASE_URL만 교체하면 됩니다.
object ApiConfig {
    // [수정됨] 배포 서버 주소로 설정.
    // 로컬 테스트 시: "http://10.0.2.2:8080/" (에뮬레이터에서 PC의 localhost 접근)
    const val BASE_URL = "http://3.39.237.57:8080/"

    // [추가됨] API 요청 타임아웃 설정 (초)
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
}
