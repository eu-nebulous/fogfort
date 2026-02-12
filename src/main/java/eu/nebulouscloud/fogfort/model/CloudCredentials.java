/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a set of credentials for authentication and access.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Entity
@Table(name = "CREDENTIALS")
public class CloudCredentials implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "CREDENTIALS_ID")
	@JsonProperty("credentialsId")
	private Integer credentialsId;

	@Column(name = "USER")
	@JsonProperty("user")
	private String user;

	@Column(name = "SECRET", columnDefinition = "CLOB")
	@JsonProperty("secret")
	private String secret;

	@Column(name = "PROJECT_ID")
	@JsonProperty("projectId")
	private String projectId;

	@Column(name = "PASSWORD")
	@JsonProperty("password")
	private String password;

	@Column(name = "PRIVATE_KEY", columnDefinition = "CLOB")
	@JsonProperty("privateKey")
	private String privateKey;

	@Column(name = "PUBLIC_KEY", columnDefinition = "CLOB")
	@JsonProperty("publicKey")
	private String publicKey;

	@Column(name = "DOMAIN")
	@JsonProperty("domain")
	private String domain;

	@Column(name = "SUBSCRIPTION_ID")
	@JsonProperty("subscriptionId")
	private String subscriptionId;
}
