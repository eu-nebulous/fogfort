/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents the configuration of a cloud.
 */
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CloudConfiguration implements Serializable {

	@Column(name = "NODE_GROUP")
	@JsonProperty("nodeGroup")
	private String nodeGroup = null;

	@Column(name = "PROPERTIES")
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@JsonProperty("properties")
	private Map<String, String> properties = null;
}
