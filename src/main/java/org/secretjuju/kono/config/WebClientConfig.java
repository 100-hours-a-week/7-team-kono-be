package org.secretjuju.kono.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient() {
		return WebClient.builder().baseUrl("https://kapi.kakao.com")
				.defaultHeader("Content-Type", "application/x-www-form-urlencoded").build();
	}
}