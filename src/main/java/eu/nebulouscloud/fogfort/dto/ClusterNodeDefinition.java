/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClusterNodeDefinition {

	@JsonProperty("nodeName")
	private String nodeName = null;

	@JsonProperty("nodeCandidateId")
	private String nodeCandidateId = null;

	@JsonProperty("cloudId")
	private String cloudId = null;

}
