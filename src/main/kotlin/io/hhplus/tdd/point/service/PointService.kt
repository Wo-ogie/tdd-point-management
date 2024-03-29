package io.hhplus.tdd.point.service

import io.hhplus.tdd.lock.UserLockManager
import io.hhplus.tdd.point.domain.PointHistory
import io.hhplus.tdd.point.domain.TransactionType
import io.hhplus.tdd.point.domain.UserPoint
import io.hhplus.tdd.point.repository.PointHistoryRepository
import io.hhplus.tdd.point.repository.UserPointRepository
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {

    /**
     * Id로 user point 정보를 단건 조회한다.
     *
     * @param id 조회하고자 하는 point의 id
     * @return 조회된 user point 정보
     */
    fun getPointById(id: Long): UserPoint {
        return userPointRepository.getById(id)
    }

    /**
     * User id에 해당하는 포인트 충전/이용 내역을 전부 조회한다.
     *
     * @param userId 포인트 충전/이용 내역을 조회하고자 하는 유저의 id
     * @return 조회된 포인트 충전/이용 내역 목록
     */
    fun findPointHistoriesByUserId(userId: Long): List<PointHistory> {
        return pointHistoryRepository.findAllByUserId(userId)
    }

    /**
     * 포인트를 충전한다.
     *
     * @param userId 포인트를 충전할 user의 id
     * @param amount 충전할 포인트 액수
     * @return 충전된 상태의 user point 정보
     */
    fun chargePoint(userId: Long, amount: Long): UserPoint = UserLockManager.withLock(userId) {
        val userPoint = getPointById(userId)
        val pointAfterCharge = userPointRepository.saveOrUpdate(
            id = userId,
            point = userPoint.point + amount
        )
        pointHistoryRepository.save(
            userId = userId,
            amount = amount,
            transactionType = TransactionType.CHARGE,
            updateMillis = pointAfterCharge.updateMillis
        )
        pointAfterCharge
    }

    /**
     * 포인트를 사용한다.
     * 사용한 액수만큼 포인트가 차감된다.
     *
     * @param userId 포인트를 사용하려는 user id
     * @param amount 사용할 포인트 액수
     * @return 사용한 상태의 user point 정보
     * @throws IllegalArgumentException 보유 포인트가 사용하려는 포인트보다 적은 경우
     */
    fun usePoint(userId: Long, amount: Long): UserPoint = UserLockManager.withLock(userId) {
        val userPoint = getPointById(userId)
        val pointDiff = userPoint.point - amount
        require(pointDiff >= 0) { "보유 포인트(${userPoint.point}p)가 부족합니다." }
        val pointAfterUse = userPointRepository.saveOrUpdate(id = userId, point = pointDiff)
        pointHistoryRepository.save(
            userId = userId,
            amount = amount,
            transactionType = TransactionType.USE,
            updateMillis = pointAfterUse.updateMillis
        )
        pointAfterUse
    }
}