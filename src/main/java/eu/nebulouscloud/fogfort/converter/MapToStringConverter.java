/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.converter;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;

@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, String>, String> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static Class<? extends HashMap> typeReference = new HashMap<String, String>().getClass();

	@Override
	public String convertToDatabaseColumn(Map<String, String> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert Map to JSON string", e);
		}
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return new HashMap<>();
		}
		try {
			return objectMapper.readValue(dbData, typeReference);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert JSON string to Map", e);
		}
	}
}
