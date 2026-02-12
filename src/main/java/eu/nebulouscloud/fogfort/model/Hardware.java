/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a hardware offer by a cloud or edge device
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@Entity
@ToString
@Table(name = "HARDWARE", indexes = { @Index(name = "idx_hardware_id", columnList = "ID") })
public class Hardware implements Serializable {

	@Id
	@Column(name = "ID", unique = true, nullable = false)
	@JsonProperty("id")
	private String id = null;

	@Column(name = "NAME")
	@JsonProperty("name")
	private String name = null;

	@Column(name = "PROVIDER_ID")
	@JsonProperty("providerId")
	private String providerId = null;

	@Column(name = "CORES")
	@JsonProperty("cores")
	private Integer cores = null;

	@Column(name = "CPU_FREQUENCY")
	@JsonProperty("cpuFrequency")
	private Double cpuFrequency = null;

	@Column(name = "RAM")
	@JsonProperty("ram")
	private Long ram = null;

	@Column(name = "DISK")
	@JsonProperty("disk")
	private Double disk = null;

	@Column(name = "GPU")
	@JsonProperty("gpu")
	private Integer gpu = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JsonProperty("location")
	private Location location = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "CLOUD_ID")
	@JsonProperty("cloud")
	@JsonIdentityReference(alwaysAsId = true)
	private Cloud cloud = null;

	@Column(name = "OWNER")
	@JsonProperty("owner")
	private String owner = null;

	@Column(name = "ARCHITECTURE")
	@JsonProperty("architecture")
	private OperatingSystemArchitecture architecture = null;

}
