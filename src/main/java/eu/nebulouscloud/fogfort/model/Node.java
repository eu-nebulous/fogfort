/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "NODE")
public class Node {
	public enum NodeStatus {
		PENDING("PENDING"),
		CREATED("CREATED"),
		FAILED("FAILED");
		private final String value;

		NodeStatus(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return value;
		}

		@JsonCreator
		public static NodeStatus fromValue(String text) {
			for (NodeStatus type : NodeStatus.values()) {
				if (type.value.equalsIgnoreCase(text)) {
					return type;
				}
			}
			return null;
		}
	}

	@Id
	@GeneratedValue(generator = "system-uuid")
	@Column(name = "ID")
	@JsonProperty("id")
	protected String id = null;

	@Column(name = "PROVIDER_ID")
	@JsonProperty("providerId")
	protected String providerId = null;

	@Column(name = "NAME")
	@JsonProperty("name")
	protected String name = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
	@JsonProperty("nodeCandidate")
	protected NodeCandidate nodeCandidate = null;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "ipAddress.ipAddressType", column = @Column(name = "SSH_IP_ADDRESS_TYPE")),
			@AttributeOverride(name = "ipAddress.ipVersion", column = @Column(name = "SSH_IP_VERSION")),
			@AttributeOverride(name = "ipAddress.value", column = @Column(name = "SSH_IP_ADDRESS_VALUE")) })
	private SSHConnectionParameters sshConnectionParameters = null;

	@Column(name = "NODE_URL")
	@JsonProperty("nodeUrl")
	private String nodeUrl = null;


	@Column(name = "NODE_STATUS")
	@JsonProperty("nodeStatus")
	@Enumerated(EnumType.STRING)
	private NodeStatus status = NodeStatus.PENDING;
}
