/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.nebulouscloud.fogfort.dto.GeoLocation;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Attributes defining this node
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@Embeddable
@ToString
public class NodeProperties implements Serializable {

	// Fields - edge node properties fields are mapped to node candidates so field
	// names are reused
	@JsonProperty("providerId")
	private String providerId = null;

	@JsonProperty("price")
	private Double price = null;

	@JsonProperty("cores")
	private Integer cores = null;

	@JsonProperty("cpuFrequency")
	private Double cpuFrequency = null;

	@JsonProperty("ram")
	private Long ram = null;

	@JsonProperty("disk")
	private Double disk = null;

	@JsonProperty("fpga")
	private Integer fpga = null;

	@JsonProperty("gpu")
	private Integer gpu = null;

	@Embedded
	@JsonProperty("OperatingSystem")
	private OperatingSystem operatingSystem = null;

	@Embedded
	@JsonProperty("geoLocation")
	private GeoLocation geoLocation = null;

}
