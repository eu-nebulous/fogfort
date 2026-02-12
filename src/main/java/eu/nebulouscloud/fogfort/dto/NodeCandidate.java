/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class NodeCandidate implements Serializable {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("nodeCandidateType")
	private NodeType nodeCandidateType = null;

	@JsonProperty("jobIdForEdge")
	private String jobIdForEDGE;

	@JsonProperty("price")
	private Double price = null;

	@JsonProperty("cloud")
	private Cloud cloud = null;

	@JsonProperty("location")
	private Location location = null;
	
	@JsonProperty("image")
	private Image image = null;

	@JsonProperty("hardware")
	private Hardware hardware = null;

	@JsonProperty("nodeId")
	private String nodeId = null;

}