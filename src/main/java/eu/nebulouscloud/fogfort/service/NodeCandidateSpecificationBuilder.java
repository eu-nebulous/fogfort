/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import eu.nebulouscloud.fogfort.dto.AttributeRequirement;
import eu.nebulouscloud.fogfort.dto.NodeType;
import eu.nebulouscloud.fogfort.dto.NodeTypeRequirement;
import eu.nebulouscloud.fogfort.dto.Requirement;
import eu.nebulouscloud.fogfort.dto.RequirementOperator;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.model.OperatingSystemFamily;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeCandidateSpecificationBuilder {

	/**
	 * Build a JPA Specification from requirements that can be filtered at database
	 * level
	 */
	public static Specification<NodeCandidate> buildSpecification(List<Requirement> requirements) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			for (Requirement requirement : requirements) {
				if (requirement instanceof NodeTypeRequirement) {
					Predicate predicate = buildNodeTypePredicate(root, cb, (NodeTypeRequirement) requirement);
					if (predicate != null) {
						predicates.add(predicate);
					}
				} else if (requirement instanceof AttributeRequirement) {
					Predicate predicate = buildAttributePredicate(root, cb, (AttributeRequirement) requirement);
					if (predicate != null) {
						predicates.add(predicate);
					}
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static Predicate buildNodeTypePredicate(Root<NodeCandidate> root, CriteriaBuilder cb,
			NodeTypeRequirement requirement) {
		if (requirement.getNodeTypes() == null || requirement.getNodeTypes().isEmpty()) {
			return null;
		}

		// Convert NodeType list to NodeCandidateTypeEnum list
		List<NodeCandidate.NodeCandidateTypeEnum> nodeTypes = new ArrayList<>();
		for (NodeType nodeType : requirement.getNodeTypes()) {
			NodeCandidate.NodeCandidateTypeEnum candidateType = mapNodeTypeToCandidateType(nodeType);
			if (candidateType != null) {
				nodeTypes.add(candidateType);
			}
		}

		if (nodeTypes.isEmpty()) {
			return null;
		}

		return root.get("nodeCandidateType").in(nodeTypes);
	}

	private static NodeCandidate.NodeCandidateTypeEnum mapNodeTypeToCandidateType(NodeType nodeType) {
		switch (nodeType) {
		case IAAS:
			return NodeCandidate.NodeCandidateTypeEnum.IAAS;
		case EDGE:
			return NodeCandidate.NodeCandidateTypeEnum.EDGE;
		default:
			return null;
		}
	}

	private static Predicate buildAttributePredicate(Root<NodeCandidate> root, CriteriaBuilder cb,
			AttributeRequirement requirement) {
		String requirementClass = requirement.getRequirementClass();
		if (requirementClass == null) {
			return null;
		}

		requirementClass = requirementClass.toLowerCase(Locale.ROOT);
		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		switch (requirementClass) {
		case "hardware":
			return buildHardwarePredicate(root, cb, attribute, value, operator);
		case "location":
			return buildLocationPredicate(root, cb, attribute, value, operator);
		case "image":
			return buildImagePredicate(root, cb, attribute, value, operator);
		case "cloud":
			return buildCloudPredicate(root, cb, attribute, value, operator);
		default:
			// environment, name - cannot be filtered at DB level easily
			return null;
		}
	}

	private static Predicate buildHardwarePredicate(Root<NodeCandidate> root, CriteriaBuilder cb, String attribute,
			String value, RequirementOperator operator) {
		Join<NodeCandidate, eu.nebulouscloud.fogfort.model.Hardware> hardwareJoin = root.join("hardware",
				JoinType.INNER);

		attribute = attribute.toLowerCase(Locale.ROOT);

		switch (attribute) {
		case "ram":
			return buildNumericPredicate(cb, hardwareJoin.get("ram"), value, operator, Long.class);
		case "cores":
			return buildNumericPredicate(cb, hardwareJoin.get("cores"), value, operator, Integer.class);
		case "disk":
			return buildNumericPredicate(cb, hardwareJoin.get("disk"), value, operator, Double.class);
		case "cpufrequency":
			return buildNumericPredicate(cb, hardwareJoin.get("cpuFrequency"), value, operator, Double.class);
		case "gpu":
			return buildNumericPredicate(cb, hardwareJoin.get("gpu"), value, operator, Integer.class);
		case "name":
			return buildStringPredicate(cb, hardwareJoin.get("name"), value, operator);
		default:
			return null;
		}
	}

	private static Predicate buildLocationPredicate(Root<NodeCandidate> root, CriteriaBuilder cb, String attribute,
			String value, RequirementOperator operator) {
		Join<NodeCandidate, eu.nebulouscloud.fogfort.model.Location> locationJoin = root.join("location",
				JoinType.INNER);

		if ("name".equalsIgnoreCase(attribute)) {
			return buildStringPredicate(cb, locationJoin.get("name"), value, operator);
		}
		// geoLocation.country - cannot be easily filtered at DB level (embedded)
		return null;
	}

	private static Predicate buildImagePredicate(Root<NodeCandidate> root, CriteriaBuilder cb, String attribute,
			String value, RequirementOperator operator) {
		Join<NodeCandidate, eu.nebulouscloud.fogfort.model.Image> imageJoin = root.join("image", JoinType.INNER);

		if ("name".equalsIgnoreCase(attribute)) {
			return buildStringPredicate(cb, imageJoin.get("name"), value, operator);
		} else if ("operatingsystem.family".equalsIgnoreCase(attribute)
				|| "operatingSystem.family".equalsIgnoreCase(attribute)) {
			// Access nested operatingSystem.family
			Join<eu.nebulouscloud.fogfort.model.Image, eu.nebulouscloud.fogfort.model.OperatingSystem> osJoin = imageJoin
					.join("operatingSystem", JoinType.INNER);
			return buildOperatingSystemFamilyPredicate(cb, osJoin.get("operatingSystemFamily"), value, operator);
		} else if ("operatingsystem.version".equalsIgnoreCase(attribute)
				|| "operatingSystem.version".equalsIgnoreCase(attribute)) {
			// Access nested operatingSystem.version
			Join<eu.nebulouscloud.fogfort.model.Image, eu.nebulouscloud.fogfort.model.OperatingSystem> osJoin = imageJoin
					.join("operatingSystem", JoinType.INNER);
			return buildNumericPredicate(cb, osJoin.get("operatingSystemVersion"), value, operator, BigDecimal.class);
		}
		return null;
	}

	private static Predicate buildCloudPredicate(Root<NodeCandidate> root, CriteriaBuilder cb, String attribute,
			String value, RequirementOperator operator) {
		Join<NodeCandidate, eu.nebulouscloud.fogfort.model.Cloud> cloudJoin = root.join("cloud", JoinType.INNER);

		if ("id".equalsIgnoreCase(attribute)) {
			return buildStringPredicate(cb, cloudJoin.get("cloudId"), value, operator);
		} else if ("type".equalsIgnoreCase(attribute)) {
			// Map CloudProviderType to CloudType string and filter
			// For now, we'll filter by cloudProvider directly
			// This might need adjustment based on actual CloudType enum
			return buildCloudTypePredicate(cb, cloudJoin.get("cloudProvider"), value, operator);
		}
		return null;
	}

	private static Predicate buildCloudTypePredicate(CriteriaBuilder cb,
			jakarta.persistence.criteria.Path<eu.nebulouscloud.fogfort.dto.CloudProviderType> path, String value,
			RequirementOperator operator) {
		// Map CloudType string to CloudProviderType
		eu.nebulouscloud.fogfort.dto.CloudProviderType providerType = mapCloudTypeToProviderType(value);
		if (providerType == null) {
			return null;
		}

		if (operator == RequirementOperator.EQ) {
			return cb.equal(path, providerType);
		} else if (operator == RequirementOperator.NEQ) {
			return cb.notEqual(path, providerType);
		}
		return null;
	}

	private static eu.nebulouscloud.fogfort.dto.CloudProviderType mapCloudTypeToProviderType(String cloudType) {
		if (cloudType == null) {
			return null;
		}
		String upperType = cloudType.toUpperCase(Locale.ROOT);
		if ("EDGE".equals(upperType)) {
			return eu.nebulouscloud.fogfort.dto.CloudProviderType.EDGE;
		}
		// For PUBLIC/PRIVATE/BYON, we can't map directly without more info
		// Return null to indicate this filter should be done in Java
		return null;
	}

	private static Predicate buildOperatingSystemFamilyPredicate(CriteriaBuilder cb,
			jakarta.persistence.criteria.Path<OperatingSystemFamily> path, String value, RequirementOperator operator) {
		try {
			OperatingSystemFamily expectedFamily = OperatingSystemFamily.fromValue(value);
			if (expectedFamily == null) {
				return null;
			}

			if (operator == RequirementOperator.EQ) {
				return cb.equal(path, expectedFamily);
			} else if (operator == RequirementOperator.NEQ) {
				return cb.notEqual(path, expectedFamily);
			} else if (operator == RequirementOperator.IN) {
				// IN operator - parse comma-separated values
				String[] values = value.split(",");
				List<OperatingSystemFamily> families = new ArrayList<>();
				for (String v : values) {
					OperatingSystemFamily family = OperatingSystemFamily.fromValue(v.trim());
					if (family != null) {
						families.add(family);
					}
				}
				if (!families.isEmpty()) {
					return path.in(families);
				}
			}
		} catch (Exception e) {
			log.warn("Error building OperatingSystemFamily predicate: {}", e.getMessage());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Number & Comparable<T>> Predicate buildNumericPredicate(CriteriaBuilder cb,
			jakarta.persistence.criteria.Path<?> path, String value, RequirementOperator operator, Class<T> type) {
		if (operator == RequirementOperator.IN) {
			// IN operator - parse comma-separated values
			String[] values = value.split(",");
			List<T> numericValues = new ArrayList<>();
			for (String v : values) {
				try {
					T numValue = parseNumeric(v.trim(), type);
					if (numValue != null) {
						numericValues.add(numValue);
					}
				} catch (NumberFormatException e) {
					// Skip invalid values
				}
			}
			if (!numericValues.isEmpty()) {
				return path.in(numericValues);
			}
			return null;
		}

		try {
			T expectedValue = parseNumeric(value, type);
			if (expectedValue == null) {
				return null;
			}

			jakarta.persistence.criteria.Path<T> typedPath = (jakarta.persistence.criteria.Path<T>) path;

			switch (operator) {
			case EQ:
				return cb.equal(typedPath, expectedValue);
			case NEQ:
				return cb.notEqual(typedPath, expectedValue);
			case LEQ:
				return cb.le(typedPath, expectedValue);
			case GEQ:
				return cb.ge(typedPath, expectedValue);
			case LT:
				return cb.lt(typedPath, expectedValue);
			case GT:
				return cb.gt(typedPath, expectedValue);
			default:
				return null;
			}
		} catch (NumberFormatException e) {
			log.warn("Invalid numeric value: {}", value);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Number & Comparable<T>> T parseNumeric(String value, Class<T> type) {
		if (type.equals(Integer.class)) {
			return (T) Integer.valueOf(value);
		} else if (type.equals(Long.class)) {
			return (T) Long.valueOf(value);
		} else if (type.equals(Double.class)) {
			return (T) Double.valueOf(value);
		} else if (type.equals(BigDecimal.class)) {
			return (T) new BigDecimal(value);
		}
		return null;
	}

	private static Predicate buildStringPredicate(CriteriaBuilder cb, jakarta.persistence.criteria.Path<String> path,
			String value, RequirementOperator operator) {
		switch (operator) {
		case EQ:
			return cb.equal(cb.lower(path), value.toLowerCase(Locale.ROOT));
		case NEQ:
			return cb.notEqual(cb.lower(path), value.toLowerCase(Locale.ROOT));
		case IN:
			// IN operator - parse comma-separated values
			String[] values = value.split(",");
			List<String> lowerValues = Arrays.stream(values).map(v -> v.trim().toLowerCase(Locale.ROOT))
					.collect(java.util.stream.Collectors.toList());
			return cb.lower(path).in(lowerValues);
		case INC:
			// INC (includes) - LIKE operation
			return cb.like(cb.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
		default:
			return null;
		}
	}
}
