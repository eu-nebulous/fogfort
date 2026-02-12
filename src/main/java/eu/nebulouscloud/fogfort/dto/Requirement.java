/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.dto;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * polymorphic Superclass, only subtypes are allowed
 */
@Getter
@Setter
@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = Requirement.JSON_TYPE, visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = AttributeRequirement.class, name = "AttributeRequirement"),
		@JsonSubTypes.Type(value = NodeTypeRequirement.class, name = "NodeTypeRequirement") })
@ToString
public abstract class Requirement {

	// JSON property constants
	public static final String JSON_TYPE = "type";

	/**
	 * Requirement type (can be ATTRIBUTE or NODE_TYPE)
	 */
	public enum RequirementType {
		ATTRIBUTE("AttributeRequirement"), NODE_TYPE("NodeTypeRequirement");

		private final String value;

		RequirementType(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return String.valueOf(value);
		}

		@JsonCreator
		public static RequirementType fromValue(String text) {
			for (RequirementType b : RequirementType.values()) {
				if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
					return b;
				}
			}
			return null;
		}
	}

	@JsonProperty(JSON_TYPE)
	protected RequirementType type;

}
