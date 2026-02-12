/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RequirementOperator {
	EQ("EQ"), LEQ("LEQ"), GEQ("GEQ"), GT("GT"), LT("LT"), NEQ("NEQ"), IN("IN"), INC("INC");

	private String value;

	private RequirementOperator(String value) {
		this.value = value;
	}

	@Override
	@JsonValue
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static RequirementOperator fromValue(String text) {
		for (RequirementOperator b : RequirementOperator.values()) {
			if (String.valueOf(b.value).equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}