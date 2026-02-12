/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.nebulouscloud.fogfort.converter.MapToStringConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "CLUSTER")
public class Cluster {

	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Column(name = "CLUSTER_ID")
	@JsonProperty("clusterId")
	private String clusterId = null;

	@Column(name = "NAME")
	@JsonProperty("name")
	private String name = null;

	@Column(name = "MASTER_NODE_NAME")
	@JsonProperty("master-node")
	private String masterNodeName;

	@Column(name = "NODES")
	@JsonProperty("nodes")
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.REFRESH)
	private List<Node> nodes;

	@Column(name = "STATUS")
	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	private ClusterStatus status = ClusterStatus.DEFINED;

	@Column(name = "ENV", columnDefinition = "CLOB")
	@JsonProperty("env-vars")
	@Convert(converter = MapToStringConverter.class)
	private Map<String, String> envVars;

	public Node getMasterNode() {
		return nodes.stream().filter(node -> node.getName().equals(masterNodeName)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Master node not found"));
	}

	public List<Node> getWorkerNodes() {
		return nodes.stream().filter(node -> !node.getName().equals(masterNodeName)).collect(Collectors.toList());
	}
}
