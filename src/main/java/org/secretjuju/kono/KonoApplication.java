package org.secretjuju.kono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.sentry.Sentry;

@SpringBootApplication
public class KonoApplication {

	@Value("${sentry.dsn}")
	private String sentryDsn;

	public static void main(String[] args) {
		SpringApplication.run(KonoApplication.class, args);
	}

	@Bean
	public ApplicationRunner initSentry() {
		return args -> {
			Sentry.init(options -> {
				options.setDsn(sentryDsn);
				options.setTracesSampleRate(1.0);
			});
		};
	}
}
