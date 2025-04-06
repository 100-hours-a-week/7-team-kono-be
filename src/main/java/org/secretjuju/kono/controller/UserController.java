package org.secretjuju.kono.controller;

import java.util.List;
import java.util.Map;

import org.secretjuju.kono.dto.request.CoinRequestDto;
import org.secretjuju.kono.dto.request.NicknameUpdateRequest;
import org.secretjuju.kono.dto.request.ProfileImageUpdateRequest;
import org.secretjuju.kono.dto.request.UserRequestDto;
import org.secretjuju.kono.dto.response.ApiResponseDto;
import org.secretjuju.kono.dto.response.CoinInfoResponseDto;
import org.secretjuju.kono.dto.response.ErrorResponse;
import org.secretjuju.kono.dto.response.UserResponseDto;
import org.secretjuju.kono.entity.User;
import org.secretjuju.kono.exception.PermissionDeniedException;
import org.secretjuju.kono.exception.UnauthorizedException;
import org.secretjuju.kono.exception.UserNotFoundException;
import org.secretjuju.kono.service.CoinFavoriteService;
import org.secretjuju.kono.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
// @RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final CoinFavoriteService coinFavoriteService;
	public UserController(UserService userService, CoinFavoriteService coinFavoriteService) {
		this.userService = userService;
		this.coinFavoriteService = coinFavoriteService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
		if (oauth2User == null) {
			return ResponseEntity.status(401).build();
		}

		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		UserResponseDto userInfo = userService.getUserInfo(kakaoId);

		return ResponseEntity.ok(userInfo);
	}

	@GetMapping("")
	public UserResponseDto getUsernickname(@AuthenticationPrincipal OAuth2User oauth2User) {
		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		UserRequestDto userRequestDto = new UserRequestDto(kakaoId);
		UserResponseDto userResponseDto = userService.getUserById(userRequestDto);
		return userResponseDto;
	}

	@PutMapping("/nickname")
	public ResponseEntity<?> updateNickname(@AuthenticationPrincipal OAuth2User oauth2User,
			@RequestBody NicknameUpdateRequest request) {
		try {
			// 인증 확인
			if (oauth2User == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("status", 401, "message", "Authentication required"));
			}

			// 닉네임 유효성 검사
			if (!isValidNickname(request.getNickname())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorResponse("닉네임은 2~10자의 한글, 영문, 숫자만 사용할 수 있어요."));
			}

			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("로그인이 필요합니다."));
			}

			// 닉네임 중복 확인
			if (userService.isNicknameExists(request.getNickname())) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("이미 사용 중인 닉네임입니다."));
			}

			// 닉네임 업데이트
			userService.updateNickname(user, request.getNickname());
			return ResponseEntity.ok().build();

		} catch (Exception e) {
			log.error("닉네임 변경 중 오류 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("닉네임 변경에 실패했습니다."));
		}
	}

	private boolean isValidNickname(String nickname) {
		// 닉네임 유효성 검사 로직
		return nickname != null && nickname.length() >= 2 && nickname.length() <= 10
				&& nickname.matches("^[a-zA-Z0-9가-힣]*$");
	}

	@GetMapping("/favorites")
	public ResponseEntity<ApiResponseDto<List<CoinInfoResponseDto>>> getUserFavorites(
			@AuthenticationPrincipal OAuth2User oauth2User) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with id: " + kakaoId);
			}

			// 관심 코인 목록 조회
			List<CoinInfoResponseDto> favoriteCoins = coinFavoriteService.getFavoriteCoinsDto(user.getId());

			// 빈 목록이어도 200 OK와 함께 빈 배열 반환
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin list retrieved", favoriteCoins));

		} catch (UserNotFoundException e) {
			throw e; // GlobalExceptionHandler로 전달
		} catch (Exception e) {
			log.error("관심 코인 조회 중 오류 발생: {}", e.getMessage(), e);
			throw e;
		}
	}

	@GetMapping("/favorites/{ticker}")
	public ResponseEntity<ApiResponseDto<Boolean>> isFavoriteCoin(@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable String ticker) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}
		CoinRequestDto coinRequestDto = new CoinRequestDto(ticker); // ticker를 CoinInfo엔티티에서 id 값으로 변환해주어야함

		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		User user = userService.getUserByKakaoId(kakaoId);

		// 즐겨찾기 여부를 boolean으로 반환하도록 수정
		boolean isFavorite = coinFavoriteService.isFavorite(user.getId(), ticker);

		return ResponseEntity.ok(new ApiResponseDto<>("Coin favorite status retrieved", isFavorite));
	}

	// 관심 코인 등록
	@PostMapping("/favorites/{ticker}")
	public ResponseEntity<ApiResponseDto<Void>> addFavoriteCoin(@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable String ticker) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("관심 코인 등록 요청: kakaoId={}, ticker={}", kakaoId, ticker);

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}

			coinFavoriteService.addFavoriteCoin(user.getId(), ticker);
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin added successfully", null));

		} catch (UserNotFoundException e) {
			throw e;
			// 구체적인 exception 구현해야함
		} catch (IllegalArgumentException e) {
			log.error("관심 코인 등록 중 잘못된 인자: {}", e.getMessage());
			throw e;
		} catch (IllegalStateException e) {
			log.error("관심 코인 등록 중 중복 오류: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("관심 코인 등록 중 오류 발생", e);
			throw e;
		}
	}

	// 관심 코인 삭제
	@DeleteMapping("/favorites/{ticker}")
	public ResponseEntity<ApiResponseDto<Void>> deleteFavoriteCoin(@AuthenticationPrincipal OAuth2User oauth2User,
			@PathVariable String ticker) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("관심 코인 삭제 요청: kakaoId={}, ticker={}", kakaoId, ticker);

			// 사용자 정보 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}

			coinFavoriteService.deleteFavoriteCoin(user.getId(), ticker);
			return ResponseEntity.ok(new ApiResponseDto<>("Favorite coin removed successfully", null));

		} catch (UserNotFoundException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			log.error("관심 코인 삭제 중 잘못된 인자: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("관심 코인 삭제 중 오류 발생", e);
			throw e;
		}
	}
	// 회원 탈퇴
	@DeleteMapping("/withdraw")
	public ResponseEntity<Void> withdrawUser(@AuthenticationPrincipal OAuth2User oauth2User, HttpSession session) {
		if (oauth2User == null) {
			return ResponseEntity.status(401).build();
		}

		try {
			Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
			log.info("회원탈퇴 요청: kakaoId={}", kakaoId);
			// 사용자의 존재여부 확인
			User user = userService.getUserByKakaoId(kakaoId);
			if (user == null) {
				throw new UserNotFoundException("User not found with kakaoId: " + kakaoId);
			}
			// 탈퇴 권한을 갖고있지 않을떄 (탈퇴하려는 회원이 자신인지 검토)
			if (!userService.hasWithdrawPermission(user)) {
				throw new PermissionDeniedException("Permission required");
			}

			// UserService의 withdrawUser 메서드 호출
			userService.withdrawUser(kakaoId);

			// 세션 무효화
			session.invalidate();

			// 204 No Content 응답
			return ResponseEntity.noContent().build();

		} catch (Exception e) {
			log.error("회원탈퇴 처리 중 오류 발생", e);
			return ResponseEntity.status(500).build();
		}
	}
	// 유저 이미지 URL 변경
	@PutMapping("/profile-image")
	public ResponseEntity<ApiResponseDto<UserResponseDto>> updateProfileImage(
			@AuthenticationPrincipal OAuth2User oauth2User, @RequestBody ProfileImageUpdateRequest request) {

		if (oauth2User == null) {
			throw new UnauthorizedException("Authentication required");
		}

		Long kakaoId = Long.valueOf(oauth2User.getAttribute("id").toString());
		log.info("프로필 이미지 업데이트 요청: kakaoId={}, imageUrl={}", kakaoId, request.getImageUrl());

		try {
			UserResponseDto updatedUser = userService.updateProfileImage(kakaoId, request);
			return ResponseEntity.ok(new ApiResponseDto<>("Profile image updated", updatedUser));
		} catch (Exception e) {
			log.error("프로필 이미지 업데이트 중 오류 발생", e);
			throw e;
		}
	}
}