/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.nebulouscloud.fogfort.dto.AttributeRequirement;
import eu.nebulouscloud.fogfort.dto.NodeType;
import eu.nebulouscloud.fogfort.dto.NodeTypeRequirement;
import eu.nebulouscloud.fogfort.dto.Requirement;
import eu.nebulouscloud.fogfort.dto.RequirementOperator;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.model.Location;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.model.OperatingSystem;
import eu.nebulouscloud.fogfort.model.OperatingSystemFamily;
import eu.nebulouscloud.fogfort.repository.NodeCandidateRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("NodeCandidateService")
public class NodeCandidateService {

	@Autowired
	private NodeCandidateRepository nodeCandidateRepository;

	@Transactional(rollbackFor = { Exception.class, RuntimeException.class })
	public void safeDeleteNodeCandidate(NodeCandidate nodeCandidate) {
		try {
			if (nodeCandidate.getNodes().isEmpty()) {
				log.info("Deleting node candidate: {}", nodeCandidate.getId());
				nodeCandidateRepository.delete(nodeCandidate);
			} else {
				log.info("Marking node candidate as inactive: {}", nodeCandidate.getId());
				nodeCandidate.setActive(false);
				nodeCandidateRepository.save(nodeCandidate);
			}
		} catch (Exception e) {
			log.error("Error deleting node candidate: {}", e.getMessage());
			throw e;
		}
	}

	public void safeDeleteNodeCandidatesByCloudId(String cloudId) {
		for (NodeCandidate nodeCandidate : nodeCandidateRepository.findByCloudId(cloudId)) {
			try {
				safeDeleteNodeCandidate(nodeCandidate);

			} catch (Exception e) {
				log.error("Error deleting node candidate: {}", e.getMessage());

			}
		}
	}

	/**
	 * Find node candidates based on requirements
	 * 
	 * @param sessionId    A valid session id
	 * @param requirements List of NodeType or Attribute requirements
	 * @return A list of all node candidates that satisfy the requirements
	 */
	public List<NodeCandidate> findNodeCandidates(String sessionId, List<Requirement> requirements) {
		log.info("Finding node candidates with {} requirements", requirements.size());

		// Separate requirements into DB-filterable and Java-filterable
		List<Requirement> dbFilterableRequirements = new java.util.ArrayList<>();
		List<Requirement> javaFilterableRequirements = new java.util.ArrayList<>();

		for (Requirement requirement : requirements) {
			if (canFilterAtDatabaseLevel(requirement)) {
				dbFilterableRequirements.add(requirement);
			} else {
				javaFilterableRequirements.add(requirement);
			}
		}

		log.info("{} requirements can be filtered at DB level, {} need Java filtering", dbFilterableRequirements.size(),
				javaFilterableRequirements.size());

		// Apply database-level filtering
		List<NodeCandidate> candidates;
		if (!dbFilterableRequirements.isEmpty()) {
			Specification<NodeCandidate> spec = NodeCandidateSpecificationBuilder
					.buildSpecification(dbFilterableRequirements);
			candidates = nodeCandidateRepository.findAll(spec);
			log.info("Found {} node candidates after DB filtering", candidates.size());
		} else {
			candidates = nodeCandidateRepository.findAll();
			log.info("Found {} total node candidates (no DB filtering)", candidates.size());
		}

		// Apply Java-level filtering for complex requirements
		if (!javaFilterableRequirements.isEmpty()) {
			candidates = candidates.stream()
					.filter(candidate -> matchesAllRequirements(candidate, javaFilterableRequirements))
					.collect(Collectors.toList());
			log.info("Found {} node candidates after Java filtering", candidates.size());
		}

		log.info("Final result: {} node candidates matching all requirements", candidates.size());
		return candidates;
	}

	/**
	 * Check if a requirement can be filtered at the database level
	 */
	private boolean canFilterAtDatabaseLevel(Requirement requirement) {
		if (requirement instanceof NodeTypeRequirement) {
			return true; // NodeType can always be filtered at DB level
		}

		if (requirement instanceof AttributeRequirement) {
			AttributeRequirement attrReq = (AttributeRequirement) requirement;
			String requirementClass = attrReq.getRequirementClass();
			if (requirementClass == null) {
				return false;
			}

			requirementClass = requirementClass.toLowerCase();
			String attribute = attrReq.getRequirementAttribute();

			switch (requirementClass) {
			case "hardware":
				// All hardware attributes can be filtered at DB level
				return true;
			case "location":
				// Only name can be filtered at DB level, geoLocation.country cannot
				return "name".equalsIgnoreCase(attribute);
			case "image":
				// All image attributes can be filtered at DB level
				return true;
			case "cloud":
				// Cloud id and type can be filtered at DB level
				return true;
			case "environment":
				// Environment cannot be filtered at DB level (not in model)
				return false;
			case "name":
				// Name/placementName might need special handling
				return false;
			default:
				return false;
			}
		}

		return false;
	}

	/**
	 * Check if a node candidate matches all requirements
	 */
	private boolean matchesAllRequirements(NodeCandidate candidate, List<Requirement> requirements) {
		for (Requirement requirement : requirements) {
			if (!matchesRequirement(candidate, requirement)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if a node candidate matches a single requirement
	 */
	private boolean matchesRequirement(NodeCandidate candidate, Requirement requirement) {
		if (requirement instanceof NodeTypeRequirement) {
			return matchesNodeTypeRequirement(candidate, (NodeTypeRequirement) requirement);
		} else if (requirement instanceof AttributeRequirement) {
			return matchesAttributeRequirement(candidate, (AttributeRequirement) requirement);
		}
		return false;
	}

	/**
	 * Check if a node candidate matches a NodeTypeRequirement
	 */
	private boolean matchesNodeTypeRequirement(NodeCandidate candidate, NodeTypeRequirement requirement) {
		if (requirement.getNodeTypes() == null || requirement.getNodeTypes().isEmpty()) {
			return true;
		}

		NodeCandidate.NodeCandidateTypeEnum candidateType = candidate.getNodeCandidateType();
		if (candidateType == null) {
			return false;
		}

		// Map NodeCandidateTypeEnum to NodeType
		NodeType candidateNodeType = mapToNodeType(candidateType);
		return requirement.getNodeTypes().contains(candidateNodeType);
	}

	/**
	 * Map NodeCandidateTypeEnum to NodeType
	 */
	private NodeType mapToNodeType(NodeCandidate.NodeCandidateTypeEnum candidateType) {
		switch (candidateType) {
		case IAAS:
			return NodeType.IAAS;
		case EDGE:
			return NodeType.EDGE;
		default:
			return null;
		}
	}

	/**
	 * Check if a node candidate matches an AttributeRequirement
	 */
	private boolean matchesAttributeRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		String requirementClass = requirement.getRequirementClass();
		if (requirementClass == null) {
			return false;
		}

		requirementClass = requirementClass.toLowerCase(Locale.ROOT);

		switch (requirementClass) {
		case "hardware":
			return matchesHardwareRequirement(candidate, requirement);
		case "location":
			return matchesLocationRequirement(candidate, requirement);
		case "image":
			return matchesImageRequirement(candidate, requirement);
		case "cloud":
			return matchesCloudRequirement(candidate, requirement);
		case "environment":
			return matchesEnvironmentRequirement(candidate, requirement);
		case "name":
			return matchesNameRequirement(candidate, requirement);
		default:
			log.warn("Unknown requirement class: {}", requirementClass);
			return false;
		}
	}

	/**
	 * Check hardware requirements (ram, cores, disk, cpuFrequency, fpga, gpu, name)
	 */
	private boolean matchesHardwareRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		Hardware hardware = candidate.getHardware();
		if (hardware == null) {
			return false;
		}

		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		switch (attribute.toLowerCase(Locale.ROOT)) {
		case "ram":
			return compareLong(hardware.getRam(), value, operator);
		case "cores":
			return compareInteger(hardware.getCores(), value, operator);
		case "disk":
			return compareDouble(hardware.getDisk(), value, operator);
		case "cpufrequency":
			return compareDouble(hardware.getCpuFrequency(), value, operator);
		case "fpga":
			// FPGA not in current Hardware model, return false
			return false;
		case "gpu":
			return compareInteger(hardware.getGpu(), value, operator);
		case "name":
			return compareString(hardware.getName(), value, operator);
		default:
			log.warn("Unknown hardware attribute: {}", attribute);
			return false;
		}
	}

	/**
	 * Check location requirements (geoLocation.country, name)
	 */
	private boolean matchesLocationRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		Location location = candidate.getLocation();
		if (location == null) {
			return false;
		}

		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		if ("geoLocation.country".equalsIgnoreCase(attribute) || "country".equalsIgnoreCase(attribute)) {
			if (location.getGeoLocation() == null || location.getGeoLocation().getCountry() == null) {
				return false;
			}
			return compareString(location.getGeoLocation().getCountry(), value, operator);
		} else if ("name".equalsIgnoreCase(attribute)) {
			return compareString(location.getName(), value, operator);
		} else {
			log.warn("Unknown location attribute: {}", attribute);
			return false;
		}
	}

	/**
	 * Check image requirements (name, operatingSystem.family,
	 * operatingSystem.version)
	 */
	private boolean matchesImageRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		Image image = candidate.getImage();
		if (image == null) {
			return false;
		}

		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		if ("name".equalsIgnoreCase(attribute)) {
			return compareString(image.getName(), value, operator);
		} else if ("operatingsystem.family".equalsIgnoreCase(attribute)
				|| "operatingSystem.family".equalsIgnoreCase(attribute)) {
			OperatingSystem os = image.getOperatingSystem();
			if (os == null || os.getOperatingSystemFamily() == null) {
				return false;
			}
			return compareOperatingSystemFamily(os.getOperatingSystemFamily(), value, operator);
		} else if ("operatingsystem.version".equalsIgnoreCase(attribute)
				|| "operatingSystem.version".equalsIgnoreCase(attribute)) {
			OperatingSystem os = image.getOperatingSystem();
			if (os == null || os.getOperatingSystemVersion() == null) {
				return false;
			}
			return compareBigDecimal(os.getOperatingSystemVersion(), value, operator);
		} else {
			log.warn("Unknown image attribute: {}", attribute);
			return false;
		}
	}

	/**
	 * Check cloud requirements (id, type)
	 */
	private boolean matchesCloudRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		eu.nebulouscloud.fogfort.model.Cloud cloud = candidate.getCloud();
		if (cloud == null) {
			return false;
		}

		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		if ("id".equalsIgnoreCase(attribute)) {
			return compareString(cloud.getCloudId(), value, operator);
		} else if ("type".equalsIgnoreCase(attribute)) {
			// Map CloudProviderType to CloudType (PRIVATE, PUBLIC, BYON, EDGE)
			// For now, we'll map EDGE to EDGE, and others to PUBLIC
			// This might need adjustment based on actual CloudType enum if it exists
			String cloudType = mapCloudProviderTypeToCloudType(cloud.getCloudProvider());
			return compareString(cloudType, value, operator);
		} else {
			log.warn("Unknown cloud attribute: {}", attribute);
			return false;
		}
	}

	/**
	 * Map CloudProviderType to CloudType string
	 */
	private String mapCloudProviderTypeToCloudType(eu.nebulouscloud.fogfort.dto.CloudProviderType providerType) {
		if (providerType == null) {
			return null;
		}
		if (providerType == eu.nebulouscloud.fogfort.dto.CloudProviderType.EDGE) {
			return "EDGE";
		}
		// Default to PUBLIC for cloud providers
		return "PUBLIC";
	}

	/**
	 * Check environment requirements (runtime: nodejs, python, java, dotnet, go)
	 * Note: This is not currently stored in NodeCandidate model, so we return false
	 */
	private boolean matchesEnvironmentRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		// Environment/runtime information is not currently stored in the NodeCandidate
		// model
		// This would need to be added to the model or retrieved from elsewhere
		log.debug("Environment requirement not supported yet - runtime information not in NodeCandidate model");
		return false;
	}

	/**
	 * Check name requirements (placementName for BYON and EDGE nodes)
	 */
	private boolean matchesNameRequirement(NodeCandidate candidate, AttributeRequirement requirement) {
		String attribute = requirement.getRequirementAttribute();
		String value = requirement.getValue();
		RequirementOperator operator = requirement.getRequirementOperator();

		if ("placementname".equalsIgnoreCase(attribute) || "placementName".equalsIgnoreCase(attribute)) {
			// For BYON and EDGE nodes, placementName might be stored in nodeId or another
			// field
			// For now, we'll check nodeId as a placeholder
			return compareString(candidate.getNodeId(), value, operator);
		} else {
			log.warn("Unknown name attribute: {}", attribute);
			return false;
		}
	}

	// Comparison helper methods

	private boolean compareString(String actual, String expected, RequirementOperator operator) {
		if (actual == null) {
			return false;
		}

		switch (operator) {
		case EQ:
			return actual.equalsIgnoreCase(expected);
		case NEQ:
			return !actual.equalsIgnoreCase(expected);
		case IN:
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> actual.equalsIgnoreCase(v.trim()));
		case INC:
			// INC (includes) - check if actual contains expected
			return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
		default:
			log.warn("String comparison operator {} not supported for attribute", operator);
			return false;
		}
	}

	private boolean compareInteger(Integer actual, String expected, RequirementOperator operator) {
		if (actual == null) {
			return false;
		}
		if (operator == RequirementOperator.IN) {
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> {
				try {
					return actual.equals(Integer.parseInt(v.trim()));
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}
		try {
			int expectedValue = Integer.parseInt(expected);
			return compareNumeric(actual, expectedValue, operator);
		} catch (NumberFormatException e) {
			log.warn("Invalid integer value: {}", expected);
			return false;
		}
	}

	private boolean compareLong(Long actual, String expected, RequirementOperator operator) {
		if (actual == null) {
			return false;
		}
		if (operator == RequirementOperator.IN) {
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> {
				try {
					return actual.equals(Long.parseLong(v.trim()));
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}
		try {
			long expectedValue = Long.parseLong(expected);
			return compareNumeric(actual, expectedValue, operator);
		} catch (NumberFormatException e) {
			log.warn("Invalid long value: {}", expected);
			return false;
		}
	}

	private boolean compareDouble(Double actual, String expected, RequirementOperator operator) {
		if (actual == null) {
			return false;
		}
		if (operator == RequirementOperator.IN) {
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> {
				try {
					return actual.equals(Double.parseDouble(v.trim()));
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}
		try {
			double expectedValue = Double.parseDouble(expected);
			return compareNumeric(actual, expectedValue, operator);
		} catch (NumberFormatException e) {
			log.warn("Invalid double value: {}", expected);
			return false;
		}
	}

	private boolean compareBigDecimal(BigDecimal actual, String expected, RequirementOperator operator) {
		if (actual == null) {
			return false;
		}
		if (operator == RequirementOperator.IN) {
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> {
				try {
					return actual.compareTo(new BigDecimal(v.trim())) == 0;
				} catch (NumberFormatException e) {
					return false;
				}
			});
		}
		try {
			BigDecimal expectedValue = new BigDecimal(expected);
			return compareNumeric(actual, expectedValue, operator);
		} catch (NumberFormatException e) {
			log.warn("Invalid BigDecimal value: {}", expected);
			return false;
		}
	}

	private <T extends Number & Comparable<T>> boolean compareNumeric(T actual, T expected,
			RequirementOperator operator) {
		int comparison = actual.compareTo(expected);
		switch (operator) {
		case EQ:
			return comparison == 0;
		case NEQ:
			return comparison != 0;
		case LEQ:
			return comparison <= 0;
		case GEQ:
			return comparison >= 0;
		case LT:
			return comparison < 0;
		case GT:
			return comparison > 0;
		default:
			log.warn("Numeric comparison operator {} not supported", operator);
			return false;
		}
	}

	private boolean compareOperatingSystemFamily(OperatingSystemFamily actual, String expected,
			RequirementOperator operator) {
		if (actual == null) {
			return false;
		}

		String actualValue = actual.toString();
		switch (operator) {
		case EQ:
			return actualValue.equalsIgnoreCase(expected);
		case NEQ:
			return !actualValue.equalsIgnoreCase(expected);
		case IN:
			// IN operator expects comma-separated values
			String[] values = expected.split(",");
			return Arrays.stream(values).anyMatch(v -> actualValue.equalsIgnoreCase(v.trim()));
		default:
			log.warn("OperatingSystemFamily comparison operator {} not supported", operator);
			return false;
		}
	}
}
