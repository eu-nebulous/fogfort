/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.io.Serializable;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class Location implements Serializable {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("name")
	private String name = null;

	@JsonProperty("providerId")
	private String providerId = null;

	@JsonProperty("locationScope")
	private Location.LocationScopeEnum locationScope = null;

	@JsonProperty("isAssignable")
	private Boolean isAssignable = null;

	@JsonProperty("geoLocation")
	private GeoLocation geoLocation = null;

	@JsonProperty("parent")
	private Location parent = null;

	@JsonProperty("owner")
	private String owner = null;

	public static enum LocationScopeEnum {
		PROVIDER("PROVIDER"), REGION("REGION"), ZONE("ZONE"), HOST("HOST");

		private final String value;

		private LocationScopeEnum(String value) {
			this.value = value;
		}

		@JsonValue
		public String toString() {
			return String.valueOf(this.value);
		}

		@JsonCreator
		public static Location.LocationScopeEnum fromValue(String text) {
			Location.LocationScopeEnum[] var1 = values();
			int var2 = var1.length;

			for (int var3 = 0; var3 < var2; ++var3) {
				Location.LocationScopeEnum b = var1[var3];
				if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
					return b;
				}
			}

			return null;
		}
	}

}