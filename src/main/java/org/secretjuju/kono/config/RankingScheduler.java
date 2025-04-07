package org.secretjuju.kono.config;

import org.secretjuju.kono.service.CoinPriceService;
import org.secretjuju.kono.service.RankingService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
public class RankingScheduler {
	private final RankingService rankingService;
	private final CoinPriceService coinPriceService;

	public RankingScheduler(RankingService rankingService, CoinPriceService coinPriceService) {
		this.rankingService = rankingService;
		this.coinPriceService = coinPriceService;
	}

	// 애플리케이션 시작 시 실행되는 초기화 메소드
	/*
	 * @PostConstruct public void initialize() {
	 * log.info("애플리케이션 시작 - 초기 랭킹 시스템 업데이트 실행"); updateRankingSystem(); }
	 * 
	 * @Scheduled(cron = "0 0/5 * * * *") public void updateRankingSystem() { try {
	 * log.info("코인 가격 업데이트 시작: {}", java.time.LocalDateTime.now()); boolean
	 * priceUpdateSuccess = updateCoinPrices();
	 * 
	 * if (priceUpdateSuccess) { log.info("랭킹 업데이트 시작: {}",
	 * java.time.LocalDateTime.now()); updateRankings();
	 * log.info("랭킹 시스템 업데이트 완료: {}", java.time.LocalDateTime.now()); } else {
	 * log.warn("코인 가격 업데이트 실패로 랭킹 업데이트를 건너뜁니다."); } } catch (Exception e) {
	 * log.error("랭킹 시스템 업데이트 중 오류 발생", e); } }
	 * 
	 * private boolean updateCoinPrices() { try {
	 * coinPriceService.updateAllCoinPrices(); log.info("코인 가격 업데이트 완료"); return
	 * true; } catch (Exception e) { log.error("코인 가격 업데이트 중 오류 발생", e); return
	 * false; } }
	 * 
	 * public void updateRankings() { try { rankingService.updateRankings(); } catch
	 * (Exception e) { log.error("랭킹 업데이트 중 오류 발생", e); } }
	 * 
	 * @Scheduled(cron = "0 59 23 * * *") // 매일 23:59에 실행 public void
	 * updateLastDayAssets() { try { coinPriceService.updateAllCoinPrices();
	 * rankingService.updateLastDayAssets(); } catch (Exception e) {
	 * log.error("어제 총 자산 업데이트 중 오류 발생", e); } }
	 */
}
