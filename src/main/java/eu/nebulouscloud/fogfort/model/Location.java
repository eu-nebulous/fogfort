/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import eu.nebulouscloud.fogfort.dto.GeoLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a (virtual) location offered by a cloud
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "LOCATION", indexes = @Index(name = "idx_location_id", columnList = "ID"))
@Accessors(chain = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class Location implements Serializable {

	@Id
	@Column(name = "ID", nullable = false, unique = true)
	@JsonProperty("id")
	private String id = null;

	@Column(name = "NAME")
	@JsonProperty("name")
	private String name = null;

	@Column(name = "PROVIDER_ID")
	@JsonProperty("providerId")
	private String providerId = null;

	/**
	 * Scope of the location
	 */
	public enum LocationScopeEnum {
		PROVIDER("PROVIDER"), REGION("REGION"), ZONE("ZONE"), HOST("HOST");

		private final String value;

		LocationScopeEnum(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static LocationScopeEnum fromValue(String text) {
			for (LocationScopeEnum b : LocationScopeEnum.values()) {
				if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
					return b;
				}
			}
			return null;
		}
	}

	@Column(name = "LOCATION_SCOPE")
	@Enumerated(EnumType.STRING)
	@JsonProperty("locationScope")
	private LocationScopeEnum locationScope = null;

	@Embedded
	@JsonProperty("geoLocation")
	private GeoLocation geoLocation = null;

}
