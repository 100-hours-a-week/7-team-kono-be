package org.secretjuju.kono.service;

import java.time.Duration;
import java.util.UUID;

import org.secretjuju.kono.dto.response.PresignedUrlResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucketName;

	@Value("${cloud.aws.s3.base-url}")
	private String baseUrl;

	@PostConstruct
	public void init() {
		log.info("S3Service initialized with bucket: {}", bucketName);
		log.info("Base URL: {}", baseUrl);
	}

	public PresignedUrlResponseDto generatePresignedUrl(String fileName, String contentType) {
		String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(uniqueFileName)
					.contentType(contentType).build();

			// Presigned URL 요청 생성 (5분 유효)
			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(5)).putObjectRequest(putObjectRequest).build();

			// Presigned URL 생성
			PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
			String presignedUrl = presignedRequest.url().toString();

			// 업로드 완료 후 접근 가능한 URL 생성
			String uploadedFileUrl;
			if (baseUrl.endsWith("/")) {
				uploadedFileUrl = baseUrl + uniqueFileName;
			} else {
				uploadedFileUrl = baseUrl + "/" + uniqueFileName;
			}

			log.info("Generated presigned URL for file: {}", fileName);
			return new PresignedUrlResponseDto(presignedUrl, uniqueFileName, uploadedFileUrl);
		} catch (Exception e) {
			log.error("Error generating presigned URL: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to generate presigned URL", e);
		}
	}
}