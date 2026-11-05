package com.coffee.gu.lock

import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ReservationResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.redisson.Redisson
import org.redisson.api.RType
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import java.net.Socket

@EnabledIf("isRedisAvailable")
class RedisLimitedMenuStockReservationManagerIntegrationTest {

    companion object {
        private lateinit var redissonClient: RedissonClient
        private lateinit var manager: RedisLimitedMenuStockReservationManager
        private const val TEST_MENU_ID = 99999L

        @JvmStatic
        fun isRedisAvailable(): Boolean {
            return runCatching {
                Socket("127.0.0.1", 6379).use { true }
            }.getOrDefault(false)
        }

        @JvmStatic
        @BeforeAll
        fun initAll() {
            if (isRedisAvailable()) {
                val config = Config().apply {
                    useSingleServer()
                        .setAddress("redis://127.0.0.1:6379")
                        .setConnectionMinimumIdleSize(2)
                        .setConnectionPoolSize(5)
                }
                redissonClient = Redisson.create(config)
                manager = RedisLimitedMenuStockReservationManager(redissonClient)
            }
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            if (::redissonClient.isInitialized) {
                redissonClient.shutdown()
            }
        }
    }

    @BeforeEach
    @AfterEach
    fun cleanUpKeys() {
        redissonClient.keys.deleteByPattern("menu:limited:{$TEST_MENU_ID}:*")
    }

    @Test
    @DisplayName("재고 초기화 후 가용 재고 조회 시 정확한 수량을 반환한다")
    fun initializeAndGetAvailableStock() {
        manager.initializeStock(TEST_MENU_ID, 10)

        val available = manager.getAvailableStock(TEST_MENU_ID)

        assertThat(available).isEqualTo(10)
    }

    @Test
    @DisplayName("선점 실행 후 각 키 인덱스별로 실제 Redis에 올바른 자료구조 타입이 생성되어야 한다 (키 순서 계약 검증)")
    fun reserveCreatesKeysWithExactDataTypesAtExpectedIndices() {
        manager.initializeStock(TEST_MENU_ID, 10)

        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-TYPE-TEST",
            principalKey = "USER-TYPE-TEST",
            quantity = 2,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        val keys = manager.keysFor(TEST_MENU_ID, 1000L)

        // 0번(KEYS[1]): STRING (Redisson RType.OBJECT)
        assertThat(redissonClient.keys.getType(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_STOCK]))
            .isEqualTo(RType.OBJECT)

        // 1번(KEYS[2]): ZSET (Redisson RType.ZSET)
        assertThat(redissonClient.keys.getType(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_EXPIRY]))
            .isEqualTo(RType.ZSET)

        // 2번(KEYS[3]): HASH (Redisson RType.MAP)
        assertThat(redissonClient.keys.getType(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_QTY]))
            .isEqualTo(RType.MAP)

        // 3번(KEYS[4]): HASH (Redisson RType.MAP)
        assertThat(redissonClient.keys.getType(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_USER]))
            .isEqualTo(RType.MAP)

        // 4번(KEYS[5]): SET (Redisson RType.SET)
        assertThat(redissonClient.keys.getType(keys[RedisLimitedMenuStockReservationManager.KEY_INDEX_ACTIVE_USERS]))
            .isEqualTo(RType.SET)
    }

    @Test
    @DisplayName("정상 선점 시 가용 재고가 차감되고 RESERVED를 반환한다")
    fun reserveSuccess() {
        manager.initializeStock(TEST_MENU_ID, 5)

        val result = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-001",
            principalKey = "USER-001",
            quantity = 2,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        assertThat(result).isEqualTo(ReservationResult.RESERVED)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(3)
    }

    @Test
    @DisplayName("동일 유저가 재선점 시도 시 ALREADY_PURCHASED를 반환하며 재고는 차감되지 않는다")
    fun duplicateUserBlocked() {
        manager.initializeStock(TEST_MENU_ID, 5)

        val firstResult = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-001",
            principalKey = "USER-001",
            quantity = 1,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )
        assertThat(firstResult).isEqualTo(ReservationResult.RESERVED)

        val secondResult = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-002",
            principalKey = "USER-001",
            quantity = 1,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        assertThat(secondResult).isEqualTo(ReservationResult.ALREADY_PURCHASED)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(4)
    }

    @Test
    @DisplayName("일자가 변경되면(다음날) 어제 구매했던 동일 유저도 다시 정상 선점할 수 있다 (1인 1일 1회 초기화 검증)")
    fun nextDaySameUserCanReserveAgain() {
        manager.initializeStock(TEST_MENU_ID, 5)

        val day1Millis = 1000L // 1970-01-01 KST
        val day2Millis = 1000L + 86_400_000L // 1970-01-02 KST (다음 날)

        // Day 1: 주문 및 선점 성공
        val day1Result = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-DAY1",
            principalKey = "USER-DAILY-1",
            quantity = 1,
            ttlMillis = 60000L,
            nowEpochMillis = day1Millis,
        )
        assertThat(day1Result).isEqualTo(ReservationResult.RESERVED)

        // Day 1: 결제 승인 완료(확정) 처리
        val confirmResult = manager.verifyAndConfirm(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-DAY1",
            nowEpochMillis = day1Millis + 1000L,
        )
        assertThat(confirmResult).isEqualTo(ConfirmationResult.CONFIRMED)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, day1Millis + 1000L)).isEqualTo(4)

        // Day 1: 같은 날 추가 주문 시 차단
        val day1Duplicate = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-DAY1-2",
            principalKey = "USER-DAILY-1",
            quantity = 1,
            ttlMillis = 60000L,
            nowEpochMillis = day1Millis + 2000L,
        )
        assertThat(day1Duplicate).isEqualTo(ReservationResult.ALREADY_PURCHASED)

        // Day 2: 다음 날이 되면 동일 유저가 다시 1개 선점 가능
        val day2Result = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-DAY2",
            principalKey = "USER-DAILY-1",
            quantity = 1,
            ttlMillis = 60000L,
            nowEpochMillis = day2Millis,
        )
        assertThat(day2Result).isEqualTo(ReservationResult.RESERVED)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, day2Millis)).isEqualTo(3)
    }

    @Test
    @DisplayName("동일 주문번호로 재호출 시 멱등하게 RESERVED를 반환하고 재고를 추가 차감하지 않는다")
    fun idempotentReserve() {
        manager.initializeStock(TEST_MENU_ID, 5)

        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-001",
            principalKey = "USER-001",
            quantity = 2,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        val retryResult = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-001",
            principalKey = "USER-001",
            quantity = 2,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        assertThat(retryResult).isEqualTo(ReservationResult.RESERVED)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(3)
    }

    @Test
    @DisplayName("잔여 재고보다 많은 수량 요청 시 SOLD_OUT을 반환한다")
    fun soldOut() {
        manager.initializeStock(TEST_MENU_ID, 2)

        val result = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-001",
            principalKey = "USER-001",
            quantity = 3,
            ttlMillis = 60000L,
            nowEpochMillis = 1000L,
        )

        assertThat(result).isEqualTo(ReservationResult.SOLD_OUT)
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(2)
    }

    @Test
    @DisplayName("선점 TTL이 만료된 후 다음 요청 발생 시 만료된 재고가 가용 재고로 자동 회수(Lazy Cleanup)된다")
    fun expiredReservationLazyCleanup() {
        manager.initializeStock(TEST_MENU_ID, 5)

        // 1000ms 시점에 선점 (TTL 10000ms -> 11000ms에 만료)
        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-EXP-1",
            principalKey = "USER-EXP-1",
            quantity = 2,
            ttlMillis = 10000L,
            nowEpochMillis = 1000L,
        )
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(3)

        // 20000ms (만료 후) 시점에 가용 재고 조회 시 만료된 2개가 가용 재고로 환원되어 5개가 됨
        val availableAfterExpiry = manager.getAvailableStock(TEST_MENU_ID, nowEpochMillis = 20000L)
        assertThat(availableAfterExpiry).isEqualTo(5)

        // 만료된 유저는 활성 유저 집합에서 제거되었으므로 다시 선점 가능해야 함
        val reReserveResult = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-NEW-1",
            principalKey = "USER-EXP-1",
            quantity = 1,
            ttlMillis = 10000L,
            nowEpochMillis = 20000L,
        )
        assertThat(reReserveResult).isEqualTo(ReservationResult.RESERVED)
    }

    @Test
    @DisplayName("verifyAndConfirm은 만료 전이면 CONFIRMED를 반환하고, 만료 후이면 EXPIRED를 반환하며 재고를 회수한다")
    fun verifyAndConfirm() {
        manager.initializeStock(TEST_MENU_ID, 5)

        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-CONFIRM-1",
            principalKey = "USER-1",
            quantity = 2,
            ttlMillis = 10000L,
            nowEpochMillis = 1000L,
        )

        // 1) 만료 전(5000ms) 확인 -> CONFIRMED
        val confirmResult = manager.verifyAndConfirm(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-CONFIRM-1",
            nowEpochMillis = 5000L,
        )
        assertThat(confirmResult).isEqualTo(ConfirmationResult.CONFIRMED)

        // 2) 다른 주문 만료 테스트
        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-CONFIRM-2",
            principalKey = "USER-2",
            quantity = 1,
            ttlMillis = 5000L,
            nowEpochMillis = 1000L, // 만료 시각: 6000L
        )

        // 만료 시각 지난 후(7000L) 확인 -> EXPIRED 반환 및 재고 1개 자동 복구
        val expiredResult = manager.verifyAndConfirm(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-CONFIRM-2",
            nowEpochMillis = 7000L,
        )
        assertThat(expiredResult).isEqualTo(ConfirmationResult.EXPIRED)
        // 전체 재고 5 중 CONFIRM-1에 2개 선점, CONFIRM-2의 1개는 만료 복구되어 가용 재고는 3개여야 함
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 7000L)).isEqualTo(3)
    }

    @Test
    @DisplayName("release 호출 시 선점 및 확정되었던 재고와 유저 제한이 즉시 복구된다")
    fun releaseRestoresStockAndUser() {
        manager.initializeStock(TEST_MENU_ID, 5)

        manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-REL-1",
            principalKey = "USER-REL-1",
            quantity = 2,
            ttlMillis = 10000L,
            nowEpochMillis = 1000L,
        )
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(3)

        // 주문 취소 release
        manager.release(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-REL-1",
            principalKey = "USER-REL-1",
            quantity = 2,
            nowEpochMillis = 1000L,
        )

        // 재고 원복 검증
        assertThat(manager.getAvailableStock(TEST_MENU_ID, 1000L)).isEqualTo(5)

        // 유저 제한 해제 검증 (취소 후 동일 유저가 다시 주문 가능)
        val retryResult = manager.reserve(
            menuId = TEST_MENU_ID,
            orderKey = "ORD-REL-2",
            principalKey = "USER-REL-1",
            quantity = 2,
            ttlMillis = 10000L,
            nowEpochMillis = 2000L,
        )
        assertThat(retryResult).isEqualTo(ReservationResult.RESERVED)
    }
}
