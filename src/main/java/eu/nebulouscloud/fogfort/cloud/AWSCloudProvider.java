package eu.nebulouscloud.fogfort.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import eu.nebulouscloud.fogfort.dto.CloudProviderType;
import eu.nebulouscloud.fogfort.dto.GeoLocation;
import eu.nebulouscloud.fogfort.dto.IpAddress;
import eu.nebulouscloud.fogfort.dto.IpAddressType;
import eu.nebulouscloud.fogfort.dto.IpVersion;
import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.CloudCredentials;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.model.Location;
import eu.nebulouscloud.fogfort.model.Node;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.model.OperatingSystem;
import eu.nebulouscloud.fogfort.model.OperatingSystemArchitecture;
import eu.nebulouscloud.fogfort.model.OperatingSystemFamily;
import eu.nebulouscloud.fogfort.model.SSHConnectionParameters;
import eu.nebulouscloud.fogfort.util.LogWrapper;
import eu.nebulouscloud.fogfort.util.TaskExecutionWithResult;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ArchitectureType;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

@Service
@Slf4j
public class AWSCloudProvider implements CloudProvider {

	@Override
	public CloudProviderType getType() {
		return CloudProviderType.AWS_EC2;
	}

	@Override
	public TaskExecutionWithResult<Set<String>> listAvailableRegions(Cloud cloud) {
		TaskExecutionWithResult<Set<String>> taskExecution = new TaskExecutionWithResult<Set<String>>(new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Listing available regions for cloud: {}", cloud.getCloudId());
			CloudCredentials credentials = cloud.getCredentials();
			if (credentials == null || credentials.getUser() == null || credentials.getPrivateKey() == null) {
				logWrapper.warn("AWS credentials are missing or incomplete for cloud: {}", cloud.getCloudId());
				return taskExecution
						.withException(new IllegalArgumentException("AWS credentials are required to list regions"));
			}

			logWrapper.info("Creating AWS credentials for user: {}", credentials.getUser());
			// Create AWS credentials from CloudCredentials
			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(credentials.getUser(),
					credentials.getPrivateKey());

			// Create EC2 client using us-east-1 as the default region for the
			// describeRegions call
			// This is a global operation that doesn't require a specific region
			try {
				logWrapper.info("Creating EC2 client with region: {} for cloud: {}", Region.US_EAST_1,
						cloud.getCloudId());
				try (Ec2Client ec2Client = Ec2Client.builder().region(Region.US_EAST_1)
						.credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build()) {

					// Query all regions that the user has access to
					DescribeRegionsRequest request = DescribeRegionsRequest.builder().allRegions(false) // Only return
																										// regions that
																										// are
																										// enabled for
																										// the
																										// account
							.build();

					logWrapper.info("Querying available regions from AWS for cloud: {}", cloud.getCloudId());
					DescribeRegionsResponse response = ec2Client.describeRegions(request);

					// Extract region names from the response
					Set<String> regions = response.regions().stream().map(region -> region.regionName())
							.collect(Collectors.toSet());

					logWrapper.info("Successfully retrieved {} available regions: {} for cloud: {}", regions.size(),
							regions, cloud.getCloudId());
					return taskExecution.withResult(regions);
				}
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while listing available regions for cloud: {}", cloud.getCloudId(),
						ex);
				return taskExecution.withException(new CloudProviderException("Error listing available regions", ex));
			} catch (Exception ex) {
				logWrapper.error("Exception while listing available regions for cloud: {}", cloud.getCloudId(), ex);
				return taskExecution.withException(new CloudProviderException("Error listing available regions", ex));
			}
		}
	}

	/**
	 * Creates a new node on an AWS EC2 cloud.
	 * @param cloud The cloud where the new node is to be allocated to
	 * @param nodeCandidate The specification of the node to be created
	 * @param nodeName The name of the node to be created
	 * @return The details of the newly created node
	 * This method doesn't throw an exception if the node creation fails. It returns a TaskExecutionWithResult object with the result of the node creation.
	 */
	public TaskExecutionWithResult<NodeCreationDetails> createNode(Cloud cloud, NodeCandidate nodeCandidate,
			String nodeName) {
		TaskExecutionWithResult<NodeCreationDetails> taskExecution = new TaskExecutionWithResult<NodeCreationDetails>(
				new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Creating node for cloud: {}, nodeCandidate: {}", cloud.getCloudId(),
					nodeCandidate != null ? nodeCandidate.getId() : "null");
			if (nodeCandidate == null) {
				return taskExecution.withException(new IllegalArgumentException("NodeCandidate cannot be null"));
			}
			if (nodeCandidate.getImage() == null) {
				return taskExecution.withException(new IllegalArgumentException("NodeCandidate image cannot be null"));
			}
			if (nodeCandidate.getHardware() == null) {
				return taskExecution
						.withException(new IllegalArgumentException("NodeCandidate hardware cannot be null"));
			}
			if (nodeCandidate.getLocation() == null) {
				return taskExecution
						.withException(new IllegalArgumentException("NodeCandidate location cannot be null"));
			}
			if (nodeCandidate.getLocation().getProviderId() == null) {
				return taskExecution.withException(
						new IllegalArgumentException("NodeCandidate location provider ID cannot be null"));
			}
			if (nodeCandidate.getImage().getProviderId() == null) {
				return taskExecution
						.withException(new IllegalArgumentException("NodeCandidate image provider ID cannot be null"));
			}
			if (nodeCandidate.getHardware().getProviderId() == null) {
				return taskExecution.withException(
						new IllegalArgumentException("NodeCandidate hardware provider ID cannot be null"));
			}

			if (cloud.getSshCredentials() == null || cloud.getSshCredentials().getKeyPairName() == null) {
				return taskExecution.withException(new IllegalArgumentException(
						"SSH credentials are missing or incomplete for cloud: " + cloud.getCloudId()));
			}

			if (cloud.getSecurityGroup() == null || cloud.getSecurityGroup().isEmpty()) {
				return taskExecution.withException(new IllegalArgumentException(
						"Security group is missing or empty for cloud: " + cloud.getCloudId()));
			}

			// Get required information from nodeCandidate
			String region = nodeCandidate.getLocation().getProviderId();
			String amiId = nodeCandidate.getImage().getProviderId();
			String instanceType = nodeCandidate.getHardware().getProviderId();

			if (region == null || region.isEmpty()) {
				return taskExecution.withException(new IllegalArgumentException("Region cannot be null or empty"));
			}
			if (amiId == null || amiId.isEmpty()) {
				return taskExecution.withException(new IllegalArgumentException("AMI ID cannot be null or empty"));
			}
			if (instanceType == null || instanceType.isEmpty()) {
				return taskExecution
						.withException(new IllegalArgumentException("Instance type cannot be null or empty"));
			}

			logWrapper.info("Creating EC2 instance - Region: {}, AMI: {}, InstanceType: {} for cloud: {}", region,
					amiId, instanceType, cloud.getCloudId());

			// Get AWS credentials
			CloudCredentials credentials = cloud.getCredentials();
			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(credentials.getUser(),
					credentials.getPrivateKey());

			// Get SSH key pair name if available
			String keyPairName = null;
			if (cloud.getSshCredentials() == null || cloud.getSshCredentials().getKeyPairName() == null) {
				return taskExecution.withException(new IllegalArgumentException(
						"SSH credentials are missing or incomplete for cloud: " + cloud.getCloudId()));
			}
			keyPairName = cloud.getSshCredentials().getKeyPairName();
			logWrapper.info("Using key pair: {} for cloud: {}", keyPairName, cloud.getCloudId());

			try {
				try (Ec2Client ec2Client = Ec2Client.builder().region(Region.of(region))
						.credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build()) {

					// Build RunInstancesRequest
					RunInstancesRequest.Builder requestBuilder = RunInstancesRequest.builder().imageId(amiId)
							.instanceType(
									software.amazon.awssdk.services.ec2.model.InstanceType.fromValue(instanceType))
							.minCount(1).maxCount(1);

					requestBuilder.keyName(keyPairName);
					// Add security group if available
					requestBuilder.securityGroupIds(Collections.singletonList(cloud.getSecurityGroup()));
					logWrapper.info("Using security group: {} for cloud: {}", cloud.getSecurityGroup(),
							cloud.getCloudId());

					// Add subnet if available
					if (cloud.getSubnet() != null && !cloud.getSubnet().isEmpty()) {
						requestBuilder.subnetId(cloud.getSubnet());
						logWrapper.info("Using subnet: {} for cloud: {}", cloud.getSubnet(), cloud.getCloudId());
					}

					// Add tags for the instance
					List<Tag> tags = new ArrayList<>();
					tags.add(Tag.builder().key("Name").value(nodeName).build());
					tags.add(Tag.builder().key("NodeCandidateId").value(nodeCandidate.getId()).build());
					tags.add(Tag.builder().key("CloudId").value(cloud.getCloudId()).build());

					TagSpecification tagSpec = TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(tags)
							.build();
					requestBuilder.tagSpecifications(tagSpec);

					// Launch the instance
					logWrapper.info("Launching EC2 instance in region: {} for cloud: {}", region, cloud.getCloudId());
					RunInstancesResponse response = ec2Client.runInstances(requestBuilder.build());

					if (response.instances().isEmpty()) {
						throw new CloudProviderException("Failed to create EC2 instance: no instances returned");
					}

					Instance instance = response.instances().get(0);
					String instanceId = instance.instanceId();
					logWrapper.info("EC2 instance created with ID: {} in region: {} for cloud: {}", instanceId, region,
							cloud.getCloudId());

					// Wait for the instance to be running
					logWrapper.info("Waiting for instance {} to be running in region: {} for cloud: {}", instanceId,
							region, cloud.getCloudId());
					waitForInstanceRunning(ec2Client, instanceId, logWrapper);

					// Describe the instance to get current state including public IP
					DescribeInstancesRequest describeRequest = DescribeInstancesRequest.builder()
							.instanceIds(instanceId).build();
					DescribeInstancesResponse describeResponse = ec2Client.describeInstances(describeRequest);

					if (describeResponse.reservations().isEmpty()
							|| describeResponse.reservations().get(0).instances().isEmpty()) {
						throw new CloudProviderException("Failed to describe EC2 instance: " + instanceId);
					}

					Instance runningInstance = describeResponse.reservations().get(0).instances().get(0);
					logWrapper.info("Instance {} is running. Public IP: {}, Private IP: {} for cloud: {}", instanceId,
							runningInstance.publicIpAddress(), runningInstance.privateIpAddress(), cloud.getCloudId());

					// Create and populate NodeCreationDetails object
					NodeCreationDetails nodeCreationDetails = new NodeCreationDetails();
					nodeCreationDetails.setProviderId(instanceId);
					nodeCreationDetails.setNodeUrl(runningInstance.publicIpAddress());
					SSHConnectionParameters sshParams = new SSHConnectionParameters();
					IpAddress ipAddress = new IpAddress();
					ipAddress.setIpAddressType(IpAddressType.PUBLIC_IP);
					ipAddress.setIpVersion(IpVersion.V4);
					ipAddress.setValue(runningInstance.publicIpAddress());
					sshParams.setIpAddress(ipAddress);
					sshParams.setPort("22");
					sshParams.setUsername(cloud.getSshCredentials().getUsername());
					sshParams.setKeyPairName(cloud.getSshCredentials().getKeyPairName());
					sshParams.setPrivateKey(cloud.getSshCredentials().getPrivateKey());
					nodeCreationDetails.setSshConnectionParameters(sshParams);

					// Set IP address
					/*
					 * if (runningInstance.publicIpAddress() != null) { IpAddress ipAddress = new
					 * IpAddress(); ipAddress.setIpAddressType(IpAddressType.PUBLIC_IP);
					 * ipAddress.setIpVersion(IpVersion.V4);
					 * ipAddress.setValue(runningInstance.publicIpAddress());
					 * sshParams.setIpAddress(ipAddress);
					 * node.setNodeUrl(runningInstance.publicIpAddress()); } else if
					 * (runningInstance.privateIpAddress() != null) { IpAddress ipAddress = new
					 * IpAddress(); ipAddress.setIpAddressType(IpAddressType.PRIVATE_IP);
					 * ipAddress.setIpVersion(IpVersion.V4);
					 * ipAddress.setValue(runningInstance.privateIpAddress());
					 * sshParams.setIpAddress(ipAddress);
					 * node.setNodeUrl(runningInstance.privateIpAddress());
					 * 
					 * } sshParams.setPort("22");
					 * sshParams.setUsername(cloud.getSshCredentials().getUsername());
					 * sshParams.setKeyPairName(cloud.getSshCredentials().getKeyPairName());
					 * sshParams.setPrivateKey(cloud.getSshCredentials().getPrivateKey());
					 * 
					 * node.setSshConnectionParameters(sshParams);
					 */

					// Set SSH port (default 22)

					logWrapper.info("Successfully created node for cloud: {}, instanceId: {}", cloud.getCloudId(),
							instanceId);
					return taskExecution.withResult(nodeCreationDetails);
				}
			} catch (IllegalArgumentException ex) {
				logWrapper.error("IllegalArgumentException while creating node for cloud: {}, nodeCandidate: {}",
						cloud.getCloudId(), nodeCandidate != null ? nodeCandidate.getId() : "null", ex);
				return taskExecution.withException(ex);
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while creating node for cloud: {}, nodeCandidate: {}",
						cloud.getCloudId(), nodeCandidate != null ? nodeCandidate.getId() : "null", ex);
				return taskExecution.withException(new CloudProviderException("Error creating node", ex));
			} catch (Exception ex) {
				logWrapper.error("Exception while creating node for cloud: {}, nodeCandidate: {}", cloud.getCloudId(),
						nodeCandidate != null ? nodeCandidate.getId() : "null", ex);
				return taskExecution.withException(new CloudProviderException("Error creating node", ex));
			}
		}
	}

	/**
	 * Wait for an EC2 instance to reach the running state
	 * 
	 * @param ec2Client  The EC2 client to use
	 * @param instanceId The instance ID to wait for
	 * @throws CloudProviderException If the instance fails to start or times out
	 */
	private void waitForInstanceRunning(Ec2Client ec2Client, String instanceId, LogWrapper logWrapper)
			throws CloudProviderException {
		int maxAttempts = 12; // 5 minutes max (60 * 5 seconds)
		int attempt = 0;

		while (attempt < maxAttempts) {
			try {
				DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
				DescribeInstancesResponse response = ec2Client.describeInstances(request);

				if (!response.reservations().isEmpty() && !response.reservations().get(0).instances().isEmpty()) {
					Instance instance = response.reservations().get(0).instances().get(0);
					InstanceStateName state = instance.state().name();

					if (state == InstanceStateName.RUNNING) {
						logWrapper.info("Instance {} is now running", instanceId);
						return;
					}
					/*
					 * else if (state == InstanceStateName.TERMINATED || state ==
					 * InstanceStateName.STOPPED || state == InstanceStateName.STOPPING) { throw new
					 * CloudProviderException("Instance " + instanceId + " entered invalid state: "
					 * + state); }
					 */
				}

				attempt++;
				if (attempt < maxAttempts) {
					try {
						Thread.sleep(5000); // Wait 5 seconds before next check
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new CloudProviderException("Interrupted while waiting for instance", e);
					}
				}
			} catch (Exception ex) {
				logWrapper.error("Error while waiting for instance {} to be running", instanceId, ex);
				// throw new CloudProviderException("Error waiting for instance to be running",
				// ex);
			}
		}

		throw new CloudProviderException("Timeout waiting for instance " + instanceId + " to reach running state");
	}

	@Override
	public TaskExecutionWithResult<Void> deleteNode(Cloud cloud, Node node) {
		TaskExecutionWithResult<Void> taskExecution = new TaskExecutionWithResult<Void>(new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Deleting node for cloud: {}, nodeId: {}", cloud.getCloudId(), node.getProviderId());

			// Validate inputs
			if (cloud == null) {
				return taskExecution.withException(new IllegalArgumentException("Cloud cannot be null"));
			}
			if (node == null) {
				return taskExecution.withException(new IllegalArgumentException("Node cannot be null"));
			}
			if (node.getProviderId() == null || node.getProviderId().isEmpty()) {
				return taskExecution
						.withException(new IllegalArgumentException("Node provider ID cannot be null or empty"));
			}
			if (node.getNodeCandidate() == null || node.getNodeCandidate().getLocation() == null
					|| node.getNodeCandidate().getLocation().getProviderId() == null
					|| node.getNodeCandidate().getLocation().getProviderId().isEmpty()) {
				return taskExecution.withException(
						new IllegalArgumentException("Node location provider ID cannot be null or empty"));
			}

			// Get AWS credentials
			CloudCredentials credentials = cloud.getCredentials();
			if (credentials == null || credentials.getUser() == null || credentials.getPrivateKey() == null) {
				logWrapper.warn("AWS credentials are missing or incomplete for cloud: {}", cloud.getCloudId());
				return taskExecution
						.withException(new IllegalArgumentException("AWS credentials are required to delete node"));
			}

			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(credentials.getUser(),
					credentials.getPrivateKey());

			String region = node.getNodeCandidate().getLocation().getProviderId();

			try {

				// Terminate the instance
				try (Ec2Client ec2Client = Ec2Client.builder().region(Region.of(region))
						.credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build()) {

					logWrapper.info("Terminating instance {} in region: {} for cloud: {}", node.getProviderId(), region,
							cloud.getCloudId());

					TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
							.instanceIds(node.getProviderId()).build();

					TerminateInstancesResponse terminateResponse = ec2Client.terminateInstances(terminateRequest);

					if (terminateResponse.terminatingInstances().isEmpty()) {
						logWrapper.warn(
								"Terminate request returned no instances for instanceId: {} in region: {} for cloud: {}",
								node.getProviderId(), region, cloud.getCloudId());
					} else {
						InstanceStateName newState = terminateResponse.terminatingInstances().get(0).currentState()
								.name();
						logWrapper.info(
								"Successfully initiated termination of instance {} in region: {} (new state: {}) for cloud: {}",
								node.getProviderId(), region, newState, cloud.getCloudId());
					}
				}
			} catch (IllegalArgumentException ex) {
				logWrapper.error("IllegalArgumentException while deleting node for cloud: {}, nodeId: {}",
						cloud.getCloudId(), node.getProviderId(), ex);
				return taskExecution.withException(ex);
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while deleting node for cloud: {}, nodeId: {}", cloud.getCloudId(),
						node.getProviderId(), ex);
				return taskExecution.withException(new CloudProviderException("Error deleting node", ex));
			} catch (Exception ex) {
				logWrapper.error("Exception while deleting node for cloud: {}, nodeId: {}", cloud.getCloudId(),
						node.getProviderId(), ex);
				return taskExecution.withException(new CloudProviderException("Error deleting node", ex));
			}
		}
		return taskExecution;
	}

	@Override
	public TaskExecutionWithResult<Set<Image>> getAllImages(Cloud cloud, String region) {
		TaskExecutionWithResult<Set<Image>> taskExecution = new TaskExecutionWithResult<Set<Image>>(new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Getting all images for cloud: {}", cloud.getCloudId());
			CloudCredentials credentials = cloud.getCredentials();
			if (credentials == null || credentials.getUser() == null || credentials.getPrivateKey() == null) {
				logWrapper.warn("AWS credentials are missing or incomplete for cloud: {}", cloud.getCloudId());
				return taskExecution
						.withException(new IllegalArgumentException("AWS credentials are required to list images"));
			}
			if (region == null || region.isEmpty()) {
				logWrapper.warn("Region is null or empty for cloud: {}", cloud.getCloudId());
				return taskExecution.withException(new IllegalArgumentException("Region is required to list images"));
			}

			logWrapper.info("Creating AWS credentials for user: {} for cloud: {}", credentials.getUser(),
					cloud.getCloudId());
			// Create AWS credentials from CloudCredentials
			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(credentials.getUser(),
					credentials.getPrivateKey());

			Set<Image> images = new HashSet<>();

			try {
				logWrapper.info("Creating EC2 client with region: {} for cloud: {}", region, cloud.getCloudId());
				try (Ec2Client ec2Client = Ec2Client.builder().region(Region.of(region))
						.credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build()) {

					// Query all available images owned by the account
					DescribeImagesRequest request = DescribeImagesRequest.builder()
							.filters(Filter.builder().name("state").values("available").build()).owners("self") // Include
																												// self-owned
																												// images
							.build();

					logWrapper.info("Querying available images from AWS in region: {} for cloud: {}", region,
							cloud.getCloudId());
					DescribeImagesResponse response = ec2Client.describeImages(request);
					logWrapper.info("Retrieved {} total images from AWS in region: {} for cloud: {}",
							response.images().size(), region, cloud.getCloudId());

					// Map AWS images to Image objects
					int processedCount = 0;
					int skippedNotListedCount = 0;
					int skippedNotUbuntuCount = 0;
					for (software.amazon.awssdk.services.ec2.model.Image awsImage : response.images()) {
						processedCount++;
						Image image = new Image();
						// Check if the image is to be listed
						boolean toBeListed = awsImage.tags().stream()
								.anyMatch(tag -> "listed-in-proactive".equals(tag.value()));
						if (!toBeListed) {
							logWrapper.info("Image {} is not marked to be listed, skipping for cloud: {}",
									awsImage.imageId(), cloud.getCloudId());
							skippedNotListedCount++;
							continue;
						}
						logWrapper.info("Image {} is marked to be listed, processing for cloud: {}", awsImage.imageId(),
								cloud.getCloudId());
						// Set basic image information
						image.setId(awsImage.imageId());
						image.setProviderId(awsImage.imageId());
						image.setName(awsImage.name() != null ? awsImage.name() : awsImage.imageId());
						image.setOwner(awsImage.ownerId());

						// Set operating system information
						OperatingSystem os = new OperatingSystem();

						// Map platform to OS family
						// Try to determine from image name or description
						// String name = awsImage.name() != null ? awsImage.name().toLowerCase() : "";
						// String description = awsImage.description() != null ?
						// awsImage.description().toLowerCase() : "";
						// String combined = name + " " + description;

						/*
						 * if(!combined.contains("ubuntu")) { logWrapper.
						 * info("Image {} is not Ubuntu, skipping. Name: {}, Description: {} for cloud: {}"
						 * , awsImage.imageId(), name, description, cloud.getCloudId());
						 * skippedNotUbuntuCount++; continue; }
						 */
						// Assume it is ubuntu
						os.setOperatingSystemFamily(OperatingSystemFamily.UBUNTU);

						// Map architecture
						if (awsImage.architecture() != null) {
							String arch = awsImage.architecture().name();
							if (arch.equalsIgnoreCase("X86_64")) {
								os.setOperatingSystemArchitecture(OperatingSystemArchitecture.AMD64);
							} else if (arch.equalsIgnoreCase("I386") || arch.equalsIgnoreCase("X86")) {
								os.setOperatingSystemArchitecture(OperatingSystemArchitecture.I386);
							} else if (arch.equalsIgnoreCase("ARM64")) {
								os.setOperatingSystemArchitecture(OperatingSystemArchitecture.ARM64);
							} else if (arch.equalsIgnoreCase("ARM")) {
								os.setOperatingSystemArchitecture(OperatingSystemArchitecture.ARM);
							} else {
								os.setOperatingSystemArchitecture(OperatingSystemArchitecture.UNKNOWN);
							}
						} else {
							os.setOperatingSystemArchitecture(OperatingSystemArchitecture.AMD64);
						}

						image.setOperatingSystem(os);

						// Set location information
						Location location = new Location();
						location.setId(region);
						location.setName(region);
						location.setProviderId(region);
						location.setLocationScope(Location.LocationScopeEnum.REGION);
						image.setLocation(location);

						images.add(image);
						logWrapper.info("Successfully processed image: {} ({}), architecture: {} for cloud: {}",
								awsImage.imageId(), awsImage.name(), os.getOperatingSystemArchitecture(),
								cloud.getCloudId());
					}

					logWrapper.info(
							"Successfully retrieved {} images from region {} (processed: {}, skipped not listed: {}, skipped not Ubuntu: {}) for cloud: {}",
							images.size(), region, processedCount, skippedNotListedCount, skippedNotUbuntuCount,
							cloud.getCloudId());
				}
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while getting all images for cloud: {}, region: {}",
						cloud.getCloudId(), region, ex);
				return taskExecution.withException(new CloudProviderException("Error getting all images", ex));
			} catch (Exception ex) {
				logWrapper.error("Exception while getting all images for cloud: {}, region: {}", cloud.getCloudId(),
						region, ex);
				return taskExecution.withException(new CloudProviderException("Error getting all images", ex));
			}

			return taskExecution.withResult(images);
		}
	}

	private OperatingSystemArchitecture awsArchitectureToOperatingSystemArchitecture(ArchitectureType awsArchitecture) {
		if (awsArchitecture.name().equals("X86_64".toLowerCase())) {
			return OperatingSystemArchitecture.AMD64;
		} else if (awsArchitecture.name().equals("I386".toLowerCase())
				|| awsArchitecture.name().equals("X86".toLowerCase())) {
			return OperatingSystemArchitecture.I386;
		} else if (awsArchitecture.name().equals("ARM64".toLowerCase())) {
			return OperatingSystemArchitecture.ARM64;
		} else if (awsArchitecture.name().equals("ARM".toLowerCase())) {
			return OperatingSystemArchitecture.ARM;
		} else {
			return OperatingSystemArchitecture.UNKNOWN;
		}
	}

	/**
	 * Get all hardware profiles of an cloud an nodes can be booted from.
	 * 
	 * @param cloud                The cloud whose hardware profiles are to be
	 *                             exposed
	 * @param region               The region to list the hardware profiles from
	 * @param requiredArchitecture The architecture of the hardware profiles to be
	 *                             listed. If null, all architectures will be
	 *                             listed.
	 * @return The set of bootable hardware profiles on the cloud
	 * @throws CloudProviderException If an error occurs while getting the hardware
	 *                                profiles
	 */
	@Override
	public TaskExecutionWithResult<Set<Hardware>> getAllHardwares(Cloud cloud, String region,
			OperatingSystemArchitecture requiredArchitecture) {
		TaskExecutionWithResult<Set<Hardware>> taskExecution = new TaskExecutionWithResult<Set<Hardware>>(new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Getting all hardwares for cloud: {} in region: {} with required architecture: {}",
					cloud.getCloudId(), region, requiredArchitecture != null ? requiredArchitecture.toString() : "all");
			CloudCredentials credentials = cloud.getCredentials();
			if (credentials == null || credentials.getUser() == null || credentials.getPrivateKey() == null) {
				logWrapper.warn("AWS credentials are missing or incomplete for cloud: {}", cloud.getCloudId());
				return taskExecution
						.withException(new IllegalArgumentException("AWS credentials are required to list hardwares"));
			}
			if (region == null || region.isEmpty()) {
				logWrapper.warn("Region is null or empty for cloud: {}", cloud.getCloudId());
				return taskExecution
						.withException(new IllegalArgumentException("Region is required to list hardwares"));
			}

			logWrapper.info("Creating AWS credentials for user: {} for cloud: {}", credentials.getUser(),
					cloud.getCloudId());
			// Create AWS credentials from CloudCredentials
			AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(credentials.getUser(),
					credentials.getPrivateKey());

			Set<Hardware> hardwares = new HashSet<>();
			try {
				logWrapper.info("Creating EC2 client with region: {} for cloud: {}", region, cloud.getCloudId());
				try (Ec2Client ec2Client = Ec2Client.builder().region(Region.of(region))
						.credentialsProvider(StaticCredentialsProvider.create(awsCredentials)).build()) {

					// Query all available instance types with pagination support
					String nextToken = null;
					int totalProcessedCount = 0;
					do {
						DescribeInstanceTypesRequest.Builder requestBuilder = DescribeInstanceTypesRequest.builder();
						if (nextToken != null) {
							requestBuilder.nextToken(nextToken);
						}
						DescribeInstanceTypesRequest request = requestBuilder.build();

						logWrapper.info(
								"Querying available instance types from AWS in region: {} for cloud: {} (nextToken: {})",
								region, cloud.getCloudId(), nextToken != null ? "present" : "none");
						DescribeInstanceTypesResponse response = ec2Client.describeInstanceTypes(request);
						logWrapper.info("Retrieved {} instance types from AWS in region: {} for cloud: {}",
								response.instanceTypes().size(), region, cloud.getCloudId());

						// Map AWS instance types to Hardware objects
						for (InstanceTypeInfo instanceType : response.instanceTypes()) {
							logWrapper.debug("Processing instance type: {} for cloud: {}",
									instanceType.instanceType().toString(), cloud.getCloudId());
							totalProcessedCount++;
							logWrapper.debug("Instance type: {} has {} supported architectures",
									instanceType.instanceType().toString(),
									instanceType.processorInfo().supportedArchitectures().size());
							for (ArchitectureType awsArchitecture : instanceType.processorInfo()
									.supportedArchitectures()) {
								OperatingSystemArchitecture hardwareArchitecture = awsArchitectureToOperatingSystemArchitecture(
										awsArchitecture);
								if (requiredArchitecture != null
										&& !hardwareArchitecture.equals(requiredArchitecture)) {
									continue;
								}

								Hardware hardware = new Hardware();

								// Set basic hardware information
								String instanceTypeName = instanceType.instanceType().toString();
								hardware.setId(cloud.getCloudId() + "-" + region + "-" + instanceTypeName);
								hardware.setProviderId(instanceTypeName);
								hardware.setName(instanceTypeName);
								hardware.setOwner(cloud.getCloudId());

								hardware.setArchitecture(OperatingSystemArchitecture.AMD64);

								// Set CPU information
								if (instanceType.vCpuInfo() != null) {
									hardware.setCores(instanceType.vCpuInfo().defaultVCpus());
								}
								if (instanceType.processorInfo() != null
										&& instanceType.processorInfo().supportedArchitectures() != null
										&& !instanceType.processorInfo().supportedArchitectures().isEmpty()) {
									// CPU frequency is not directly available, set to null
									hardware.setCpuFrequency(0.0);
								}

								// Set memory information (convert from MiB to bytes)
								if (instanceType.memoryInfo() != null
										&& instanceType.memoryInfo().sizeInMiB() != null) {
									hardware.setRam(instanceType.memoryInfo().sizeInMiB());
								}

								// Set disk information (convert from GB to GB, but stored as Double)
								if (instanceType.instanceStorageInfo() != null
										&& instanceType.instanceStorageInfo().totalSizeInGB() != null) {
									hardware.setDisk(instanceType.instanceStorageInfo().totalSizeInGB().doubleValue());
								} else {
									// If no instance storage, set to 0 or null
									hardware.setDisk(0.0);
								}

								// Set GPU information
								if (instanceType.gpuInfo() != null && instanceType.gpuInfo().gpus() != null
										&& !instanceType.gpuInfo().gpus().isEmpty()) {
									// Sum up all GPUs
									int totalGpus = instanceType.gpuInfo().gpus().stream()
											.mapToInt(gpu -> gpu.count() != null ? gpu.count() : 0).sum();
									hardware.setGpu(totalGpus > 0 ? totalGpus : null);
								} else {
									hardware.setGpu(0);
								}

								// Set location information
								Location location = new Location();
								location.setId(region);
								location.setName(region);
								location.setProviderId(region);
								location.setLocationScope(Location.LocationScopeEnum.REGION);
								GeoLocation geoLocation = new GeoLocation();// TODO: Get the geo location from the
																			// region
								geoLocation.setCity(region);
								geoLocation.setCountry(region);
								geoLocation.setLatitude(0.0);
								geoLocation.setLongitude(0.0);
								location.setGeoLocation(geoLocation);
								hardware.setLocation(location);

								hardwares.add(hardware);
								logWrapper.debug(
										"Successfully processed instance type: {} (cores: {}, ram: {} bytes, disk: {} GB, gpu: {}, architecture: {}) for cloud: {}",
										instanceTypeName, hardware.getCores(), hardware.getRam(), hardware.getDisk(),
										hardware.getGpu(), hardware.getArchitecture(), cloud.getCloudId());
							}
						}

						nextToken = response.nextToken();
					} while (nextToken != null);

					logWrapper.info("Successfully retrieved {} hardwares from region {} (processed: {}) for cloud: {}",
							hardwares.size(), region, totalProcessedCount, cloud.getCloudId());
				}
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while getting all hardwares for cloud: {}, region: {}",
						cloud.getCloudId(), region, ex);
				return taskExecution.withException(new CloudProviderException("Error getting all hardwares", ex));
			} catch (Exception ex) {
				logWrapper.error("Exception while getting all hardwares for cloud: {}, region: {}", cloud.getCloudId(),
						region, ex);
				return taskExecution.withException(new CloudProviderException("Error getting all hardwares", ex));
			}

			return taskExecution.withResult(hardwares);
		}
	}

	@Override
	public TaskExecutionWithResult<Void> deleteInfrastructure(Cloud cloud) {
		TaskExecutionWithResult<Void> taskExecution = new TaskExecutionWithResult<Void>(new Date());
		try (LogWrapper logWrapper = taskExecution.getLogWrapper(log)) {
			logWrapper.info("Deleting infrastructure for cloud: {}", cloud.getCloudId());

			// TODO Auto-generated method stub
			try {
				logWrapper.warn("deleteInfrastructure is not yet implemented");
				return taskExecution.withException(new CloudProviderException("Error deleting infrastructure"));
			} catch (Exception ex) {
				logWrapper.error("Exception while deleting infrastructure for cloud: {}", cloud.getCloudId(), ex);
				return taskExecution.withException(new CloudProviderException("Error deleting infrastructure", ex));
			}
		}
	}
}
