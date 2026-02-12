/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;
import java.math.BigDecimal;

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

/**
 * Represents the operating system of an image
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@ToString
public class OperatingSystem implements Serializable {

	@Column(name = "OPERATING_SYSTEM_FAMILY")
	@Enumerated(EnumType.STRING)
	@JsonProperty("operatingSystemFamily")
	private OperatingSystemFamily operatingSystemFamily = null;

	@Column(name = "OPERATING_SYSTEM_ARCHITECTURE")
	@Enumerated(EnumType.STRING)
	@JsonProperty("operatingSystemArchitecture")
	private OperatingSystemArchitecture operatingSystemArchitecture = null;

	@Column(name = "OPERATING_SYSTEM_VERSION")
	@JsonProperty("operatingSystemVersion")
	private BigDecimal operatingSystemVersion = null;

}
