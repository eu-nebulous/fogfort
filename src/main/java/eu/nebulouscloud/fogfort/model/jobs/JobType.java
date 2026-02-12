/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model.jobs;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobType {

	FETCH_CLOUD_NODE_CANDIDATES("FETCH_CLOUD_NODE_CANDIDATES"),

	DEPLOY_CLUSTER("DEPLOY_CLUSTER"),

	TERMINATE_CLUSTER("TERMINATE_CLUSTER"),

	SCALE_OUT("SCALE_OUT"),

	SCALE_IN("SCALE_IN"),

	UNKNOWN("UNKNOWN");

	private final String value;

	JobType(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static JobType fromValue(String text) {
		for (JobType b : JobType.values()) {
			if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
				return b;
			}
		}
		return UNKNOWN;
	}
}
