/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.nebulouscloud.fogfort.cloud.CloudManager;
import eu.nebulouscloud.fogfort.cloud.CloudProvider;
import eu.nebulouscloud.fogfort.cloud.CloudProviderException;
import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.CloudCredentials;
import eu.nebulouscloud.fogfort.model.CloudDefinition;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.model.Node;
import eu.nebulouscloud.fogfort.repository.CloudRepository;
import eu.nebulouscloud.fogfort.repository.CredentialsRepository;
import eu.nebulouscloud.fogfort.repository.HardwareRepository;
import eu.nebulouscloud.fogfort.repository.ImageRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("CloudService")
public class CloudService {

	@Autowired
	private CloudRepository cloudRepository;
	@Autowired
	private ImageRepository imageRepository;
	@Autowired
	private HardwareRepository hardwareRepository;
	@Autowired
	private NodeCandidateService nodeCandidateService;
	@Autowired
	private CredentialsRepository credentialsRepository;

	@Autowired
	private CloudPopulationAsyncService cloudPopulationAsyncService;

	@Autowired
	private CloudManager cloudManager;


	public Boolean refreshCloudResources(String sessionId, String cloudId) {
		Optional<Cloud> cloud = cloudRepository.findById(cloudId);
		if (cloud.isEmpty()) {
			log.warn("Cloud doesn't exist");
			return false;
		}
		nodeCandidateService.safeDeleteNodeCandidatesByCloudId(cloudId);

		cloudPopulationAsyncService.populateCloudNodeCandidates(cloud.get());
		return true;
	}

	/**
	 * Add clouds to the Resource Manager
	 * 
	 * @param sessionId A valid session id
	 * @param clouds    A list of CloudDefinition instances
	 * @return 0 if clouds has been added properly. A greater than 0 value
	 *         otherwise.
	 */
	@Transactional(rollbackFor = { CloudProviderException.class, Exception.class })
	public List<Cloud> addClouds(String sessionId, List<CloudDefinition> clouds) throws Exception {
		if (clouds == null || clouds.isEmpty())
			throw new IllegalArgumentException("The received clouds structure is empty. Nothing to be created.");

		List<Cloud> savedClouds = new LinkedList<>();
		for (CloudDefinition cloud : clouds) {
			if (!isValidCloudId(cloud.getCloudId())) {
				throw new IllegalArgumentException("Invalid cloudId: " + cloud.getCloudId()
						+ ". Must be 3-253 characters and contain only lowercase letters, numbers, and hyphens.");
			}

			if (cloudRepository.findById(cloud.getCloudId()).isPresent()) {
				throw new IllegalArgumentException("Cloud with cloudId: " + cloud.getCloudId() + " already exists");
			}

			Cloud newCloud = new Cloud();
			String nodeSourceNamePrefix = cloud.getCloudProvider() + "-" + cloud.getCloudId();
			newCloud.setNodeSourceNamePrefix(nodeSourceNamePrefix);
			newCloud.setCloudId(cloud.getCloudId());
			newCloud.setCloudProvider(cloud.getCloudProvider());
			newCloud.setSubnet(cloud.getSubnet());
			newCloud.setSecurityGroup(cloud.getSecurityGroup());
			newCloud.setEndpoint(cloud.getEndpoint());
			newCloud.setScopePrefix(cloud.getScope().getPrefix());
			newCloud.setScopeValue(cloud.getScope().getValue());
			newCloud.setIdentityVersion(cloud.getIdentityVersion());
			newCloud.setDefaultNetwork(cloud.getDefaultNetwork());
			newCloud.setBlacklist(Optional.ofNullable(cloud.getBlacklist()).orElse(""));
			newCloud.setSshCredentials(cloud.getSshCredentials());
			CloudCredentials credentials = new CloudCredentials();
			credentials.setUser(cloud.getCredentials().getUser());
			credentials.setProjectId(cloud.getCredentials().getProjectId());
			credentials.setPrivateKey(cloud.getCredentials().getSecret());
			credentials.setDomain(cloud.getCredentials().getDomain());
			credentials.setSubscriptionId(cloud.getCredentials().getSubscriptionId());
			credentialsRepository.save(credentials);
			newCloud.setCredentials(credentials);
			newCloud = cloudRepository.save(newCloud);
			log.debug("Cloud infrastructure created: " + newCloud);
			savedClouds.add(newCloud);

			// Validate cloud by listing available regions
			// If this fails, @Transactional will automatically rollback all database
			// changes
			log.info("Validating cloud: {} by listing available regions", cloud.getCloudId());
			CloudProvider cloudProvider = cloudManager.getCloudProvider(cloud.getCloudProvider());
			cloudProvider.listAvailableRegions(newCloud);
			log.info("Cloud: {} validated successfully", cloud.getCloudId());

			log.info("Scheduling asynchronous process to populate node candidates for cloud: {}", cloud.getCloudId());
			cloudPopulationAsyncService.populateCloudNodeCandidates(newCloud);

		}
		log.info("Clouds created properly.");
		return savedClouds;
	}

	private boolean isValidCloudId(String name) {
		return name != null && !name.isEmpty() && name.length() >= 3 && name.length() <= 253
				&& name.matches("^[a-z0-9-]+$");
	}

	/**
	 * Verify if there is any asynchronous fetching/cleaning node candidates process
	 * in progress
	 * 
	 * @param sessionId A valid session id
	 * @return true if at least one asynchronous node candidates process is in
	 *         progress, false otherwise
	 */
	public Boolean isAnyAsyncCloudPopulationProcessesInProgress(String sessionId) {
		return cloudPopulationAsyncService.isAnyAsyncCloudPopulationProcessesInProgress(sessionId);
	}

	/**
	 * Get all registered clouds
	 * 
	 * @param sessionId A valid session id
	 * @return List of all table PACloud's entries
	 */
	public List<Cloud> getAllClouds(String sessionId) {

		List<Cloud> clouds = new ArrayList<>();
		for (Cloud cloud : cloudRepository.findAll()) {
			clouds.add(cloud);
		}
		return clouds;
	}

	/**
	 * Get all added clouds with specific ids
	 * 
	 * @param sessionId A valid session id
	 * @param cloudIds  Valid cloud ids
	 * @return List of all table PACloud's entries
	 */
	public List<Cloud> findCloudsByIds(String sessionId, List<String> cloudIds) {

		List<Cloud> clouds = new ArrayList<>();
		for (Cloud cloud : cloudRepository.findAllById(cloudIds)) {
			clouds.add(cloud);
		}
		return clouds;
	}

	public Boolean removeClouds(String sessionId, List<String> cloudIds, Boolean preempt) {

		cloudIds.forEach(cloudId -> {
			removeCloud(sessionId, cloudId, preempt);
		});
		return true;
	}

	public Boolean undeployClouds(String sessionId, List<String> cloudIds, Boolean preempt) {

		cloudIds.forEach(cloudId -> {
			removeCloud(sessionId, cloudId, preempt);
		});
		return true;
	}

	public Boolean removeCloud(String sessionId, String cloudId, Boolean preempt) {
		Optional<Cloud> cloud = cloudRepository.findById(cloudId);
		if (cloud.isEmpty()) {
			log.warn("Cloud doesn't exist");
			return false; // Continue the loop
		}
		List<Node> toBeRemovedDeployments = new ArrayList<>();
		if (!cloud.get().getDeployments().isEmpty()) {
			log.info("Cleaning all deployments related to cloud \"{}\"", cloud.get().getCloudId());
			toBeRemovedDeployments.addAll(cloud.get().getDeployments());
			for (Node deployment : toBeRemovedDeployments) {
				// TODO: repositoryService.deleteDeployment(deployment);
			}
		}
		log.info("Cleaning node candidates");
		List<String> toBeRemovedClouds = Collections.singletonList(cloud.get().getCloudId());
		try {
			// TODO: delete node candidates
			log.info("Cleaning node candidates related to clouds {} ended properly with {} NC cleaned.",
					toBeRemovedClouds.get(0), null);
		} catch (Exception e) {
			log.warn("Cleaning node candidates for cloud {} returned an exception!", toBeRemovedClouds.get(0), e);
		}
		cloudRepository.delete(cloud.get());
		log.info("Cloud removed.");
		cloudRepository.flush();
		return true;
	}

	public List<Image> fetchCloudImages(String sessionId, String cloudId) throws Exception {
		List<Image> allImages = getAllCloudImages(sessionId);
		List<Image> filteredImages = new LinkedList<>();
		Optional<Cloud> paCloud = cloudRepository.findById(cloudId);

		if (paCloud.isEmpty()) {
			log.warn("Cloud ID '{}' is not found in SAL DB.", cloudId);
			throw new Exception(String.format("Cloud %s doesnt' exist", cloudId));
		}

		try {
			// TODO: fetch images
		} catch (RuntimeException e) {
			log.error("Failed to get images for cloud {}: {}", cloudId, e.getMessage(), e);
			// throw new InternalServerErrorException("Error while retrieving images for
			// cloud: " + cloudId, e);
		}

		return filteredImages;
	}

	/**
	 * This function returns the list of all available images
	 * 
	 * @param sessionId A valid session id
	 * @return A list of all available images
	 */
	public List<Image> getAllCloudImages(String sessionId) {
		return imageRepository.findAll();
	}

	/**
	 * This function returns the list of all available images
	 * 
	 * @param sessionId A valid session id
	 * @return A list of all available images
	 */
	public List<Image> getCloudImages(String sessionId, String cloudId) {
		return imageRepository.findAll();
	}

	/**
	 * This function returns the list of all available hardware related to a
	 * registered cloud
	 * 
	 * @param sessionId A valid session id
	 * @param cloudId   A valid cloud identifier
	 * @return A list of available hardware
	 */
	public List<Hardware> fetchCloudHardware(String sessionId, String cloudId) {

		return List.of();
	}

	/**
	 * This function returns the list of all available hardware
	 * 
	 * @param sessionId A valid session id
	 * @return A list of all available hardware
	 */
	public List<Hardware> getAllCloudHardware(String sessionId) {
		return hardwareRepository.findAll();
	}

	/**
	 * This function returns the list of all available hardware
	 * 
	 * @param sessionId A valid session id
	 * @return A list of all available hardware
	 */
	public List<Hardware> getCloudHardware(String sessionId, String cloudId) {
		return hardwareRepository.findAll();
	}

	@Transactional(rollbackFor = { Exception.class, RuntimeException.class })
	public void deleteCloud(String sessionId, String cloudId) {
		Optional<Cloud> cloud = cloudRepository.findById(cloudId);
		if (cloud.isEmpty()) {
			log.warn("Cloud doesn't exist");
			return;
		}
		if (!cloud.get().getDeployments().isEmpty()) {
			Set<String> deploymentIds = cloud.get().getDeployments().stream().map(Node::getId)
					.collect(Collectors.toSet());
			throw new IllegalArgumentException(
					"Cloud has deployments. Please undeploy the cloud before deleting it. Deployments: "
							+ deploymentIds.toString());
		}
		try {
			cloudRepository.delete(cloud.get());
		} catch (Exception e) {
			log.error("Error deleting cloud: {}", e.getMessage());
			throw e;
		}
	}

}
