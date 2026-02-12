/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import eu.nebulouscloud.fogfort.dto.CloudProviderType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "CLOUD")
@ToString
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "cloudId", scope = Cloud.class)
public class Cloud implements Serializable {

	public static final String WHITE_LISTED_NAME_PREFIX = "WLH";

	@Id
	@Column(name = "CLOUD_ID")
	private String cloudId;

	@Column(name = "NODE_SOURCE_NAME_PREFIX")
	private String nodeSourceNamePrefix;

	@Column(name = "SUBNET")
	private String subnet;

	@Column(name = "SECURITY_GROUP")
	private String securityGroup;

	@Embedded
	@Column(name = "SSH_CREDENTIALS")
	private SSHCredentials sshCredentials;

	@Column(name = "ENDPOINT")
	private String endpoint;

	@Column(name = "SCOPE_PREFIX")
	private String scopePrefix;

	@Column(name = "SCOPE_VALUE")
	private String scopeValue;

	@Column(name = "IDENTITY_VERSION")
	private String identityVersion;

	@Column(name = "DEFAULT_NETWORK")
	private String defaultNetwork;

	@Column(name = "BLACKLIST")
	private String blacklist;

	@Column(name = "CLOUD_PROVIDER")
	@Enumerated(EnumType.STRING)
	private CloudProviderType cloudProvider;

	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
	@JsonIdentityReference(alwaysAsId = true)
	@JsonProperty("deploymentNodeNames")
	private List<Node> deployments;

	@OneToMany(mappedBy = "cloud", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonIdentityReference(alwaysAsId = true)
	@JsonProperty("images")
	private List<Image> images;

	@OneToMany(mappedBy = "cloud", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonIdentityReference(alwaysAsId = true)
	@JsonProperty("hardware")
	private List<Hardware> hardware;

	@OneToMany(mappedBy = "cloud", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonIdentityReference(alwaysAsId = true)
	@JsonProperty("nodeCandidates")
	private List<NodeCandidate> nodeCandidates;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "credentials_CREDENTIALS_ID")
	private CloudCredentials credentials;

	@PreRemove
	private void cleanMappedDataFirst() {
		/*
		 * this.deployedRegions.clear(); this.deployedWhiteListedRegions.clear();
		 */
	}
}
