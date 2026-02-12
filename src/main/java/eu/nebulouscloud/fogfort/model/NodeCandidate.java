/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * A node creatable by the system
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode
@Getter
@Setter
@Entity
@ToString
@Table(name = "NODE_CANDIDATE", indexes = { @Index(name = "idx_nodecandidate_id", columnList = "ID") })
public class NodeCandidate implements Serializable {
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Column(name = "ID", nullable = false, unique = true)
	@JsonProperty("id")
	private String id = null;

	/**
	 * Gets or Sets nodeCandidateType
	 */
	public enum NodeCandidateTypeEnum {
		IAAS("IAAS"), EDGE("EDGE");

		private final String value;

		NodeCandidateTypeEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static NodeCandidateTypeEnum fromValue(String text) {
			for (NodeCandidateTypeEnum b : NodeCandidateTypeEnum.values()) {
				if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
					return b;
				}
			}
			return null;
		}
	}

	@Column(name = "NODE_CANDIDATE_TYPE")
	@Enumerated(EnumType.STRING)
	@JsonProperty("nodeCandidateType")
	private NodeCandidateTypeEnum nodeCandidateType = null;

	@Column(name = "JOB_ID_FOR_EDGE")
	@JsonProperty("jobIdForEdge")
	private String jobIdForEDGE;

	@Column(name = "PRICE")
	@JsonProperty("price")
	private Double price = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.REFRESH })
	@JsonIdentityReference(alwaysAsId = true)
	@JsonProperty("cloud")
	@JsonIgnore
	private Cloud cloud = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.REFRESH })
	@JsonProperty("location")
	private Location location = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.REFRESH })
	@JsonProperty("image")
	private Image image = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.REFRESH })
	@JsonProperty("hardware")
	private Hardware hardware = null;

	@Column(name = "PRICE_PER_INVOCATION")
	@JsonProperty("pricePerInvocation")
	private Double pricePerInvocation = null;

	@Column(name = "MEMORY_PRICE")
	@JsonProperty("memoryPrice")
	private Double memoryPrice = null;

	@Column(name = "NODE_ID")
	@JsonProperty("nodeId")
	private String nodeId = null;

	@OneToMany(mappedBy = "nodeCandidate")
	@JsonIgnore
	private List<Node> nodes = null;

	/**
	 * Check if a node candidate is of EDGE type
	 * 
	 * @return true if yes, false if not
	 */
	@JsonIgnore
	public boolean isEdgeNodeCandidate() {
		return nodeCandidateType.equals(NodeCandidateTypeEnum.EDGE);
	}

	/**
	 * If not active, the node candidate will not be reported when listing node
	 * candidates
	 */
	@Column(name = "ACTIVE")
	@JsonProperty("active")
	private Boolean active = true;

}
