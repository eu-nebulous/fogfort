/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.CloudDefinition;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.repository.CloudRepository;
import eu.nebulouscloud.fogfort.service.CloudService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST controller for managing cloud resources and operations.
 * Provides endpoints for adding, retrieving, refreshing, undeploying, and removing clouds,
 * as well as accessing cloud-related images and hardware configurations.
 * 
 * @author FogFort Team
 */
@RestController
@RequestMapping(value = "/sal/cloud")
@Api(tags = "Cloud Operations", consumes = "application/json", produces = "application/json")
public class CloudController {

	@Autowired
	private CloudService cloudService;
	@Autowired
	private CloudRepository cloudRepository;

	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(value = "Add cloud", response = Cloud.class, responseContainer = "List")
	public ResponseEntity<List<Cloud>> addCloud(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "A CloudDefinition instances in json format", required = true) @RequestBody final List<CloudDefinition> clouds)
			throws Exception {

		return ResponseEntity.ok(cloudService.addClouds(sessionId, clouds));

	}

	@RequestMapping(method = RequestMethod.GET)
	@ApiOperation(value = "Get all registered clouds", response = Cloud.class, responseContainer = "List")
	public ResponseEntity<List<Cloud>> getAllClouds(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@RequestParam(value = "cloudIds") final Optional<List<String>> cloudIds) {
		if (cloudIds.isPresent()) {
			return ResponseEntity.ok(cloudService.findCloudsByIds(sessionId, cloudIds.get()));
		} else {
			return ResponseEntity.ok(cloudService.getAllClouds(sessionId));
		}
	}

	@RequestMapping(value = "/{cloudId}", method = RequestMethod.GET)
	@ApiOperation(value = "Get a specific cloud with its images and hardware", response = Cloud.class)
	@Transactional(readOnly = true)
	public ResponseEntity<Cloud> getCloud(
			@ApiParam(value = "A cloud identifier", required = true) @PathVariable(name = "cloudId") final String cloudId) {
		Cloud cloud = cloudRepository.findById(cloudId)
				.orElseThrow(() -> new IllegalArgumentException("Cloud not found: " + cloudId));
		// Force eager loading of related entities
		if (cloud.getImages() != null) {
			cloud.getImages().size(); // Force initialization
		}
		if (cloud.getHardware() != null) {
			cloud.getHardware().size(); // Force initialization
		}
		if (cloud.getDeployments() != null) {
			cloud.getDeployments().size(); // Force initialization
		}
		return ResponseEntity.ok(cloud);
	}

	@RequestMapping(value = "/refresh", method = RequestMethod.POST)
	@ApiOperation(value = "Refresh cloud resources", response = Boolean.class)
	public ResponseEntity<Boolean> refreshCloudResources(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "A valid cloud identifier", required = true) @RequestParam(value = "cloudId") final String cloudId) {
		return ResponseEntity.ok(cloudService.refreshCloudResources(sessionId, cloudId));
	}

	@RequestMapping(value = "/undeploy", method = RequestMethod.POST)
	@ApiOperation(value = "Undeploy clouds", response = Boolean.class)
	public ResponseEntity<Boolean> undeployClouds(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "List of cloud IDs to undeploy", required = true) @RequestBody final List<String> cloudIds,
			@ApiParam(value = "If true undeploy node source immediately without waiting for nodes to be freed", defaultValue = "false") @RequestHeader(value = "preempt", defaultValue = "false") final Boolean preempt) {
		return ResponseEntity.ok(cloudService.undeployClouds(sessionId, cloudIds, preempt));
	}

	@RequestMapping(value = "/remove", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove clouds", response = Boolean.class)
	public ResponseEntity<Boolean> removeClouds(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "List of cloud IDs to remove", required = true) @RequestBody final List<String> cloudIds,
			@ApiParam(value = "If true undeploy node source immediately without waiting for nodes to be freed", defaultValue = "false") @RequestHeader(value = "preempt", defaultValue = "false") final Boolean preempt) {
		return ResponseEntity.ok(cloudService.removeClouds(sessionId, cloudIds, preempt));
	}

	@RequestMapping(value = "/images", method = RequestMethod.GET)
	@ApiOperation(value = "Get the list of all available images related to a registered cloud", response = Image.class, responseContainer = "List")
	public ResponseEntity<List<Image>> getCloudImages(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "A valid cloud identifier") @RequestParam(value = "cloudId") final Optional<String> cloudId) {
		if (cloudId.isPresent()) {
			return ResponseEntity.ok(cloudService.getCloudImages(sessionId, cloudId.get()));
		} else {
			return ResponseEntity.ok(cloudService.getAllCloudImages(sessionId));
		}
	}

	@RequestMapping(value = "/hardware", method = RequestMethod.GET)
	@ApiOperation(value = "Get the list of all available hardwares related to a registered cloud", response = Hardware.class, responseContainer = "List")
	public ResponseEntity<List<Hardware>> getCloudHardwares(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "A valid cloud identifier") @RequestParam(value = "cloudId") final Optional<String> cloudId) {
		if (cloudId.isPresent()) {
			return ResponseEntity.ok(cloudService.getCloudHardware(sessionId, cloudId.get()));
		} else {
			return ResponseEntity.ok(cloudService.getAllCloudHardware(sessionId));
		}
	}

}
