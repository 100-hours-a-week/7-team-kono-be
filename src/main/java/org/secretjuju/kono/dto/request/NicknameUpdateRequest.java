package org.secretjuju.kono.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameUpdateRequest {
	@NotBlank(message = "닉네임은 비어있을 수 없습니다.")
	@Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
	private String nickname;
}