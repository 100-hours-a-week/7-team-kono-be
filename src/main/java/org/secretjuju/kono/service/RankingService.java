package org.secretjuju.kono.service;

import java.util.List;
import java.util.stream.Collectors;

import org.secretjuju.kono.dto.response.DailyRankingResponseDto;
import org.secretjuju.kono.dto.response.TotalRankingResponseDto;
import org.secretjuju.kono.entity.Badge;
import org.secretjuju.kono.entity.CashBalance;
import org.secretjuju.kono.entity.DailyRanking;
import org.secretjuju.kono.entity.TotalRanking;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.repository.CashBalanceRepository;
import org.secretjuju.kono.repository.CoinHoldingRepository;
import org.secretjuju.kono.repository.DailyRankingRepository;
import org.secretjuju.kono.repository.TotalRankingRepository;
import org.secretjuju.kono.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {
	private final UserRepository userRepository;
	private final TotalRankingRepository totalRankingRepository;
	private final DailyRankingRepository dailyRankingRepository;
	private final CoinHoldingRepository coinHoldingRepository;
	private final CashBalanceRepository cashBalanceRepository;
	private final UserService userService;
	private final CoinService coinService;
	private final CoinPriceService coinPriceService;

	// 사용자의 총 자산 계산
	private Long calculateTotalAssets(User user) {
		// 현금 잔액
		Long cashBalance = 0L;
		if (user.getCashBalance() != null) {
			cashBalance = user.getCashBalance().getBalance();
		} else {
			// CashBalance가 없는 사용자에게 새로운 CashBalance 생성
			CashBalance newCashBalance = new CashBalance();
			newCashBalance.setUser(user);
			newCashBalance.setBalance(10000000L);
			cashBalanceRepository.save(newCashBalance);
			user.setCashBalance(newCashBalance);
			log.warn("사용자 ID: {}에 CashBalance가 없어 새로 생성했습니다.", user.getId());
			cashBalance = newCashBalance.getBalance();
		}

		// 코인 자산
		Long coinAssets = coinHoldingRepository.findByUser(user).stream().map(holding -> {
			String ticker = holding.getCoinInfo().getTicker(); // "BTC" 형식 가정
			Double currentPrice = coinPriceService.getPriceByTicker(ticker);
			return (long) (holding.getHoldingQuantity() * currentPrice);
			// Double currentPrice =
			// coinPriceService.getPriceByTicker(holding.getCoinInfo().getTicker());
			// return (long) (holding.getHoldingQuantity() * currentPrice);
		}).reduce(0L, Long::sum);

		return cashBalance + coinAssets;
	}

	// 수익률 계산
	private Double calculateProfitRate(Long currentAssets, Long lastDayAssets) {
		if (lastDayAssets == 0)
			return 0.0;
		return ((double) (currentAssets - lastDayAssets) / lastDayAssets) * 100;
	}

	@Transactional
	public void updateRankings() {
		log.info("랭킹 업데이트 시작");

		// 1. 모든 사용자 조회
		List<User> users = userRepository.findAll();

		// 2. 각 사용자의 총 자산 계산 및 랭킹 업데이트
		for (User user : users) {
			Long currentAssets = calculateTotalAssets(user);

			// TotalRanking 업데이트
			TotalRanking totalRanking = totalRankingRepository.findByUser(user).orElse(new TotalRanking(user));
			totalRanking.setCurrentTotalAssets(currentAssets);
			totalRanking.updateTime();
			totalRankingRepository.save(totalRanking);

			// DailyRanking 업데이트
			DailyRanking dailyRanking = dailyRankingRepository.findByUser(user).orElse(new DailyRanking(user));
			dailyRanking.setCurrentTotalAssets(currentAssets);
			dailyRanking.setProfitRate(calculateProfitRate(currentAssets, dailyRanking.getLastDayTotalAssets()));
			dailyRanking.updateTime();
			dailyRankingRepository.save(dailyRanking);
		}

		// 3. 전체 랭킹 순위 업데이트
		List<TotalRanking> totalRankings = totalRankingRepository.findAllByOrderByCurrentTotalAssetsDesc();
		for (int i = 0; i < totalRankings.size(); i++) {
			totalRankings.get(i).setTotalRank(i + 1);
			totalRankings.get(i).updateTime();
		}
		totalRankingRepository.saveAll(totalRankings);

		// 4. 일간 랭킹 순위 업데이트
		List<DailyRanking> dailyRankings = dailyRankingRepository.findAllByOrderByProfitRateDesc();
		for (int i = 0; i < dailyRankings.size(); i++) {
			dailyRankings.get(i).setDailyRank(i + 1);
			dailyRankings.get(i).updateTime();
		}
		dailyRankingRepository.saveAll(dailyRankings);

		log.info("랭킹 업데이트 완료");
	}

	@Transactional
	public void updateLastDayAssets() {
		log.info("어제 총 자산 업데이트 시작");

		List<DailyRanking> dailyRankings = dailyRankingRepository.findAll();
		for (DailyRanking ranking : dailyRankings) {
			ranking.setLastDayTotalAssets(ranking.getCurrentTotalAssets());
			ranking.updateTime();
		}
		dailyRankingRepository.saveAll(dailyRankings);

		log.info("어제 총 자산 업데이트 완료");
	}

	// 일간 랭킹 조회
	@Transactional(readOnly = true)
	public List<DailyRankingResponseDto> getDailyRanking() {
		// 일간 랭킹 상위 100개 조회
		List<DailyRanking> dailyRankings = dailyRankingRepository.findTop100ByOrderByDailyRankAsc();

		return dailyRankings.stream().map(this::convertToDailyRankingResponse).collect(Collectors.toList());
	}

	// 전체 랭킹 조회
	public List<TotalRankingResponseDto> getTotalRanking() {
		// 전체 랭킹 상위 100개 조회
		List<TotalRanking> totalRankings = totalRankingRepository.findTop100ByOrderByTotalRankAsc();

		return totalRankings.stream().map(this::convertToTotalRankingResponse).collect(Collectors.toList());
	}

	// 현재 사용자의 일간 랭킹 조회
	public DailyRankingResponseDto getCurrentUserDailyRanking() {
		User currentUser = userService.getCurrentUser();

		DailyRanking dailyRanking = dailyRankingRepository.findByUser(currentUser)
				.orElseThrow(() -> new EntityNotFoundException("랭킹 정보를 찾을 수 없습니다."));

		List<String> badgeImageUrls = currentUser.getBadges().stream().map(Badge::getBadgeImageUrl)
				.collect(Collectors.toList());

		DailyRankingResponseDto responseDto = DailyRankingResponseDto.builder().nickname(currentUser.getNickname())
				.profileImageUrl(currentUser.getProfileImageUrl()).badgeImageUrl(badgeImageUrls)
				.profitRate(dailyRanking.getProfitRate()).rank(dailyRanking.getDailyRank()).build();

		return responseDto;
	}

	// 현재 사용자의 전체 랭킹 조회
	public TotalRankingResponseDto getCurrentUserTotalRanking() {
		User currentUser = userService.getCurrentUser();

		TotalRanking totalRanking = totalRankingRepository.findByUser(currentUser)
				.orElseThrow(() -> new EntityNotFoundException("랭킹 정보를 찾을 수 없습니다."));

		List<String> badgeImageUrls = currentUser.getBadges().stream().map(Badge::getBadgeImageUrl)
				.collect(Collectors.toList());

		TotalRankingResponseDto responseDto = TotalRankingResponseDto.builder().nickname(currentUser.getNickname())
				.profileImageUrl(currentUser.getProfileImageUrl()).badgeImageUrl(badgeImageUrls)
				.profit(totalRanking.getProfit()).rank(totalRanking.getTotalRank()).build();

		return responseDto;
	}

	private DailyRankingResponseDto convertToDailyRankingResponse(DailyRanking dailyRanking) {
		return new DailyRankingResponseDto(dailyRanking.getUser().getNickname(),
				dailyRanking.getUser().getProfileImageUrl(),
				dailyRanking.getUser().getBadges().stream().map(Badge::getBadgeImageUrl).collect(Collectors.toList()),
				dailyRanking.getProfitRate(), dailyRanking.getDailyRank());
	}

	private TotalRankingResponseDto convertToTotalRankingResponse(TotalRanking totalRanking) {
		return TotalRankingResponseDto.builder().nickname(totalRanking.getUser().getNickname())
				.profileImageUrl(totalRanking.getUser().getProfileImageUrl())
				.badgeImageUrl(totalRanking.getUser().getBadges().stream().map(Badge::getBadgeImageUrl)
						.collect(Collectors.toList()))
				.profit(totalRanking.getProfit()).rank(totalRanking.getTotalRank()).build();
	}
}