package org.secretjuju.kono.config;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.secretjuju.kono.service.CoinPriceService;
import org.secretjuju.kono.service.RankingService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class RankingScheduler {
	private final RankingService rankingService;
	private final CoinPriceService coinPriceService;
	private final RedissonClient redissonClient;

	// 분산 락 키 정의
	private static final String RANKING_SCHEDULER_LOCK = "lock:ranking:scheduler";
	private static final String DAILY_ASSETS_SCHEDULER_LOCK = "lock:daily:assets:scheduler";

	// 락 획득 대기 시간 및 유지 시간 설정
	private static final long WAIT_TIME_SECONDS = 10;
	private static final long LEASE_TIME_SECONDS = 600; // 10분

	// 애플리케이션 시작 시 실행되는 초기화 메소드
	@PostConstruct
	public void initialize() {
		log.info("애플리케이션 시작 - 초기 랭킹 시스템 업데이트 실행");
		executeWithLock(RANKING_SCHEDULER_LOCK, this::updateRankingSystem);
	}

	@Scheduled(cron = "0 0/5 * * * *") // 5분마다 실행
	public void scheduledRankingUpdate() {
		executeWithLock(RANKING_SCHEDULER_LOCK, this::updateRankingSystem);
	}

	@Scheduled(cron = "0 59 23 * * *") // 매일 23:59에 실행
	public void scheduledDailyAssetsUpdate() {
		executeWithLock(DAILY_ASSETS_SCHEDULER_LOCK, this::updateLastDayAssets);
	}

	// 분산 락을 이용해 작업 실행
	private void executeWithLock(String lockName, Runnable task) {
		RLock lock = redissonClient.getLock(lockName);
		boolean acquired = false;

		try {
			log.debug("락 획득 시도: {}", lockName);
			acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);

			if (acquired) {
				log.info("* 락 획득 성공: {} - 작업 실행", lockName);
				task.run();
			} else {
				log.info("* 락 획득 실패: {} - 다른 서버에서 이미 작업 실행 중", lockName);
			}
		} catch (InterruptedException e) {
			log.error("락 획득 중 인터럽트 발생: {}", lockName, e);
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("작업 실행 중 예외 발생: {}", lockName, e);
		} finally {
			if (acquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.debug("락 해제 완료: {}", lockName);
			}
		}
	}

	public void updateRankingSystem() {
		try {
			log.info("코인 가격 업데이트 시작: {}", java.time.LocalDateTime.now());
			boolean priceUpdateSuccess = updateCoinPrices();

			if (priceUpdateSuccess) {
				log.info("랭킹 업데이트 시작: {}", java.time.LocalDateTime.now());
				updateRankings();
				log.info("랭킹 시스템 업데이트 완료: {}", java.time.LocalDateTime.now());
			} else {
				log.warn("코인 가격 업데이트 실패로 랭킹 업데이트를 건너뜁니다.");
			}
		} catch (Exception e) {
			log.error("랭킹 시스템 업데이트 중 오류 발생", e);
		}
	}

	private boolean updateCoinPrices() {
		try {
			coinPriceService.updateAllCoinPrices();
			log.info("코인 가격 업데이트 완료");
			return true;
		} catch (Exception e) {
			log.error("코인 가격 업데이트 중 오류 발생", e);
			return false;
		}
	}

	private void updateRankings() {
		try {
			rankingService.updateRankings();
		} catch (Exception e) {
			log.error("랭킹 업데이트 중 오류 발생", e);
		}
	}

	private void updateLastDayAssets() {
		try {
			coinPriceService.updateAllCoinPrices();
			rankingService.updateLastDayAssets();
			log.info("어제 총 자산 업데이트 완료");
		} catch (Exception e) {
			log.error("어제 총 자산 업데이트 중 오류 발생", e);
		}
	}
}