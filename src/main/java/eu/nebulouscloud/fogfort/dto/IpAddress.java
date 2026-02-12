/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * IpAddress
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@Embeddable
@ToString
public class IpAddress implements Serializable {

	@Column(name = "IP_ADDRESS_TYPE")
	@Enumerated(EnumType.STRING)
	@JsonProperty("IpAddressType")
	private IpAddressType ipAddressType = null;

	@Column(name = "IP_VERSION")
	@Enumerated(EnumType.STRING)
	@JsonProperty("IpVersion")
	private IpVersion ipVersion = null;

	@Column(name = "VALUE")
	@JsonProperty("value")
	private String value = null;

}
