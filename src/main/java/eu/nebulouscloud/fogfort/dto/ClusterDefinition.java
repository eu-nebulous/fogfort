/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class ClusterDefinition {

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("nodes")
	private List<ClusterNodeDefinition> nodes;

	@JsonProperty("master-node")
	private String masterNode;

	@JsonProperty("env-var")
	private Map<String, String> envVars;

}
