package org.secretjuju.kono.entity;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coin_transaction")
@Getter
@Setter
@NoArgsConstructor
public class CoinTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coin_info_id", nullable = false)
	private CoinInfo coinInfo;

	@Column(name = "order_type", nullable = false, length = 20)
	private String orderType;

	@Column(name = "order_quantity", nullable = false)
	private Double orderQuantity;

	@Column(name = "order_price", nullable = false)
	private Double orderPrice;

	@Column(name = "created_at", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	@Column(name = "order_amount", nullable = false, columnDefinition = "BIGINT UNSIGNED")
	private Long orderAmount;

}
