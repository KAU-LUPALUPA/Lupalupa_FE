package com.example.lupapj.data.repository

import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.InventoryItem
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.remote.ShopApiService
import com.example.lupapj.data.remote.model.ShopItemDto
import com.example.lupapj.data.remote.model.ShopItemsValidateRequest
import com.example.lupapj.data.remote.model.ShopPurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.example.lupapj.data.model.DefaultShopItems // [추가됨]

/**
 * [추가됨(권)] 서버 API를 통해 상점 데이터를 관리하는 리포지토리 구현체.
 */
class RemoteShopRepository(
    private val apiService: ShopApiService,
    private val currencyRepository: CurrencyRepository,
    private val localCache: ShopLocalCache
) : ShopRepository {

    // [수정됨] 마스터 데이터 기본값(DefaultShopItems)으로 초기화
    private val _shopItems = MutableStateFlow<List<ShopItem>>(DefaultShopItems)
    override val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    // [수정됨] 언매니지드 코루틴 누수를 방지하기 위해 자체 Scope 정의 및 stateIn 사용 (단일 진실 공급원 패턴)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override val inventory: StateFlow<List<InventoryItem>> = localCache.purchasedItemsFlow
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    override suspend fun fetchShopItems() {
        // 상점 목록 검증 API 호출
        val currentItems = _shopItems.value
        val request = ShopItemsValidateRequest(
            items = currentItems.map { ShopItemDto(it.id, it.name, it.price) }
        )

        try {
            val response = apiService.validateShopItems(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "fail" && body.failList != null) {
                    // [추가됨] 서버와 로컬 데이터(가격 등) 불일치 시 서버 기준으로 로컬 상태 동기화
                    val updatedItems = _shopItems.value.map { localItem ->
                        val failedItem = body.failList.find { it.id == localItem.id }
                        if (failedItem != null) {
                            localItem.copy(price = failedItem.price)
                        } else {
                            localItem
                        }
                    }
                    _shopItems.value = updatedItems
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun purchaseItem(itemId: String): Result<Unit> {
        val item = _shopItems.value.find { it.id == itemId } 
            ?: return Result.failure(Exception("아이템 정보를 찾을 수 없습니다."))
        
        val currentBalance = currencyRepository.currencyState.value.amount

        val request = ShopPurchaseRequest(
            itemId = itemId,
            amount = 1,
            price = item.price,
            balance = currentBalance
        )

        return try {
            val response = apiService.purchaseItem(request)
            val body = response.body()
            
            if (response.isSuccessful && body?.status == "success") {
                // 1. 재화 업데이트 (서버에서 내려준 잔액으로 동기화)
                body.balance?.let {
                    currencyRepository.syncCurrency(it)
                }

                // 2. 인벤토리 업데이트 (서버에서 내려준 전체 인벤토리 목록으로 동기화)
                val newInventory = body.inventory?.map { 
                    InventoryItem(instanceId = it.instanceId, masterId = it.masterId, count = it.count)
                } ?: emptyList()
                
                // [수정됨] 단일 진실 공급원(SSOT) 유지를 위해 _inventory.value 직접 할당 제거. 
                // 캐시에만 저장하면 StateFlow가 자동으로 감지해 UI를 업데이트함.
                localCache.savePurchasedItems(newInventory)

                Result.success(Unit)
            } else {
                Result.failure(Exception("구매 실패: 서버 응답이 올바르지 않습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 초기 아이템 목록 설정을 위한 헬퍼 (Mock 데이터 대신 서버에서 받아오는 로직으로 대체 가능)
    fun setInitialShopItems(items: List<ShopItem>) {
        _shopItems.value = items
    }
}
