/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
 * Represents an image offered by a cloud
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@Entity
@ToString
@Table(name = "IMAGE")
public class Image implements Serializable {

	@Id
	@Column(name = "ID")
	@JsonProperty("id")
	private String id = null;

	@Column(name = "NAME")
	@JsonProperty("name")
	private String name = null;

	@Column(name = "PROVIDER_ID")
	@JsonProperty("providerId")
	private String providerId = null;

	@Embedded
	@JsonProperty("operatingSystem")
	private OperatingSystem operatingSystem = null;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JsonProperty("location")
	private Location location = null;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name = "CLOUD_ID")
	@JsonProperty("cloud")
	@JsonIgnore
	private Cloud cloud = null;

	@Column(name = "OWNER")
	@JsonProperty("owner")
	private String owner = null;

}
