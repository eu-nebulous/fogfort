/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class GeoLocation implements Serializable {
	public static final String JSON_CITY = "city";
	public static final String JSON_COUNTRY = "country";
	public static final String JSON_LATITUDE = "latitude";
	public static final String JSON_LONGITUDE = "longitude";

	@JsonProperty("city")
	private String city = null;

	@JsonProperty("country")
	private String country = null;

	@JsonProperty("latitude")
	private Double latitude = null;

	@JsonProperty("longitude")
	private Double longitude = null;

}