/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("NodeTypeRequirement")
@Getter
@Setter
@ToString
public class NodeTypeRequirement extends Requirement {
	@JsonProperty("nodeTypes")
	private List<NodeType> nodeTypes;
	@JsonProperty("jobIdForEDGE")
	private String jobIdForEDGE;

	public NodeTypeRequirement() {
		this.type = RequirementType.NODE_TYPE;
	}

	public NodeTypeRequirement(List<NodeType> nodeTypes, String jobIdForEDGE) {
		this.type = RequirementType.NODE_TYPE;
		this.nodeTypes = nodeTypes;
		this.jobIdForEDGE = jobIdForEDGE;
	}

}