/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model.jobs;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JobStatus {
	CREATED("CREATED"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), CANCELLED("CANCELLED"),
	UNKNOWN("UNKNOWN");

	private final String value;

	JobStatus(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}
}
