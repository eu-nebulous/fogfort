/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents SSH credentials for cloud configuration
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Embeddable
public class SSHCredentials implements Serializable {

	@Column(name = "USERNAME")
	@JsonProperty("username")
	private String username = null;

	@Column(name = "KEY_PAIR_NAME")
	@JsonProperty("keyPairName")
	private String keyPairName = null;

	@Lob
	@Column(name = "PUBLIC_KEY")
	@JsonProperty("publicKey")
	private String publicKey = null;

	@Lob
	@Column(name = "PRIVATE_KEY")
	@JsonProperty("privateKey")
	private String privateKey = null;
}
