package com.example.lupapj.data.mock

import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.ShopCategory
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.repository.CurrencyRepository
import com.example.lupapj.data.repository.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// [추가됨(권)] 상점 기능(목록 조회 및 구매)의 서버 통신을 모사하는 Mock 객체.
// DataStore 로컬 캐시에서 인벤토리 초기값을 불러오고, 서버(Mock) 응답 성공 후에만 캐시를 갱신합니다.
class MockShopRepository(
    // [추가됨(권)] 구매 진행 시 재화를 차감하기 위해 CurrencyRepository 주입
    private val currencyRepository: CurrencyRepository,
    private val localCache: ShopLocalCache // [추가됨(권)] 로컬 캐시 주입
) : ShopRepository {

    // [추가됨(권)] 서버에서 제공할 가상의 상점 아이템 초기 데이터
    private val mockItems = listOf(
        ShopItem("item_hat_1", "밀짚모자", "여름에 쓰기 좋은 시원한 모자입니다.", 150, ShopCategory.HAT),
        ShopItem("item_glasses_1", "선글라스", "멋진 검은색 선글라스입니다.", 200, ShopCategory.GLASSES),
        ShopItem("item_clothes_1", "빨간 망토", "따뜻하고 예쁜 망토입니다.", 300, ShopCategory.CLOTHING),
        ShopItem("item_blue_shoes", "파란 신발", "가볍게 뛰어다니기 좋은 파란 신발입니다.", 180, ShopCategory.SHOES),
        ShopItem("item_accessory_1", "스카프", "바람 불 때 좋은 스카프입니다.", 100, ShopCategory.ACCESSORY)
    )

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    override val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _inventory = MutableStateFlow<List<String>>(emptyList())
    override val inventory: StateFlow<List<String>> = _inventory.asStateFlow()

    init {
        // [추가됨(권)] 테스트를 위해 초기 데이터를 바로 주입
        _shopItems.value = mockItems

        // [추가됨(권)] 앱 시작 시 DataStore에 저장된 인벤토리(구매 이력)를 불러와서 초기 상태로 설정
        CoroutineScope(Dispatchers.IO).launch {
            val cachedIds = localCache.purchasedItemIdsFlow.first()
            _inventory.value = cachedIds
        }
    }

    override suspend fun fetchShopItems() {
        delay(500) // [추가됨(권)] 네트워크 조회 지연 시간 모사
        _shopItems.value = mockItems
    }

    override suspend fun purchaseItem(itemId: String): Result<Unit> {
        delay(500) // [추가됨(권)] 구매 API 호출 지연 모사
        val item = mockItems.find { it.id == itemId } ?: return Result.failure(Exception("아이템을 찾을 수 없습니다."))
        
        // [추가됨(권)] 보유 여부 사전 검증
        if (_inventory.value.contains(itemId)) {
            return Result.failure(Exception("이미 보유한 아이템입니다."))
        }

        // [추가됨(권)] 재화 소모 검증 (서버에서 차감을 시도하는 흐름을 모사)
        val spendResult = currencyRepository.spendCurrency(item.price.toLong())
        return if (spendResult is com.example.lupapj.data.model.CurrencyUpdateResult.Success) {
            // [추가됨(권)] 재화 차감 성공 시 내 인벤토리에 추가
            val newInventory = _inventory.value + itemId
            _inventory.value = newInventory
            // [추가됨(권)] 서버(Mock) 응답 성공 후 로컬 캐시에 인벤토리 기록
            localCache.savePurchasedItemIds(newInventory)
            Result.success(Unit)
        } else {
            // [추가됨(권)] 재화 차감 실패(재화 부족) 처리
            val errorMessage = if (spendResult is com.example.lupapj.data.model.CurrencyUpdateResult.ValidationError) spendResult.message else "재화가 부족합니다."
            Result.failure(Exception(errorMessage))
        }
    }
}
