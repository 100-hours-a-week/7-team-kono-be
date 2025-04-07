package org.secretjuju.kono.exception;

import java.util.Map;

import org.apache.coyote.BadRequestException;
import org.secretjuju.kono.dto.response.ApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<?> handleCustomException(CustomException ex) {
		Sentry.captureException(ex);
		return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
	}
	@ExceptionHandler(NicknameAlreadyExistsException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleNicknameAlreadyExistsException(
			NicknameAlreadyExistsException e) {
		Sentry.captureException(e);
		log.error("닉네임 중복 예외 발생: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponseDto<>(e.getMessage(), null));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleUnauthorizedException(UnauthorizedException e) {
		Sentry.captureException(e);
		log.error("인증 오류: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDto<>(e.getMessage(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
		Sentry.captureException(e);
		log.error("유효성 검사 예외 발생: {}", e.getMessage());
		String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto<>(errorMessage, null));
	}

	@ExceptionHandler(PermissionDeniedException.class)
	public ResponseEntity<ApiResponseDto<Object>> handlePermissionDeniedException(PermissionDeniedException e) {
		Sentry.captureException(e);
		log.error("권한 오류: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseDto<>(e.getMessage(), null));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleUserNotFoundException(UserNotFoundException e) {
		Sentry.captureException(e);
		log.error("사용자를 찾을 수 없음: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDto<>(e.getMessage(), null));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleBadRequestException(BadRequestException e) {
		Sentry.captureException(e);
		log.error("잘못된 요청: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto<>(e.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Object>> handleGeneralException(Exception e) {
		Sentry.captureException(e);
		log.error("예상치 못한 오류 발생: ", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponseDto<>("Internal server error", null));
	}
}