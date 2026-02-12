/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.nebulouscloud.fogfort.model.NodeProperties;
import eu.nebulouscloud.fogfort.model.SSHCredentials;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Attributes defining an EDGE node
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString(callSuper = true)
public class EdgeDefinition {
	public static final String DEFAULT_PORT = "22";

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("systemArch")
	private String systemArch = null;

	@JsonProperty("loginCredential")
	private SSHCredentials loginCredential = null;

	@JsonProperty("ipAddresses")
	private List<IpAddress> ipAddresses = null;

	@JsonProperty("port")
	private String port = DEFAULT_PORT;

	@JsonProperty("nodeProperties")
	private NodeProperties nodeProperties = null;
}
