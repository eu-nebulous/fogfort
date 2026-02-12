package eu.nebulouscloud.fogfort.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import eu.nebulouscloud.fogfort.cloud.CloudManager;
import eu.nebulouscloud.fogfort.cloud.CloudProvider;
import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.model.jobs.FetchCloudNodeCandidatesJob;
import eu.nebulouscloud.fogfort.model.jobs.JobStatus;
import eu.nebulouscloud.fogfort.model.jobs.JobType;
import eu.nebulouscloud.fogfort.model.jobs.Task;
import eu.nebulouscloud.fogfort.repository.HardwareRepository;
import eu.nebulouscloud.fogfort.repository.ImageRepository;
import eu.nebulouscloud.fogfort.repository.JobRepository;
import eu.nebulouscloud.fogfort.repository.NodeCandidateRepository;
import eu.nebulouscloud.fogfort.repository.TaskRepository;
import eu.nebulouscloud.fogfort.util.LogWrapper;
import eu.nebulouscloud.fogfort.util.TaskExecutionWithResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service("CloudPopulationAsyncService")
@Slf4j
public class CloudPopulationAsyncService {
	@Autowired
	private CloudManager cloudManager;
	@Autowired
	private NodeCandidateRepository nodeCandidateRepository;
	@Autowired
	private ImageRepository imageRepository;
	@Autowired
	private HardwareRepository hardwareRepository;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private TaskRepository taskRepository;
	private static Map<String, Boolean> cloudNodeCandidateListTask = new ConcurrentHashMap<>();
	@Autowired
	private PlatformTransactionManager transactionManager;

	private TaskExecutionWithResult<Set<NodeCandidate>> listNodeCandidates(Cloud cloud, String region) {
		TaskExecutionWithResult<Set<NodeCandidate>> taskExecutionWithResult = new TaskExecutionWithResult<Set<NodeCandidate>>(
				new Date());
		taskExecutionWithResult.setResult(new HashSet<NodeCandidate>());
		try (LogWrapper logWrapper = taskExecutionWithResult.getLogWrapper(log)) {
			logWrapper.info("Listing node candidates for cloud: {} in region: {}", cloud.getCloudId(), region);
			CloudProvider cloudProvider = cloudManager.getCloudProvider(cloud.getCloudProvider());

			try {
				logWrapper.info("Retrieving images and hardwares for region: {} for cloud: {}", region,
						cloud.getCloudId());
				// Get all available images and hardwares for the region
				TaskExecutionWithResult<Set<Image>> imagesTaskExecution = cloudProvider.getAllImages(cloud, region);
				if (!imagesTaskExecution.isSuccess()) {
					logWrapper.error("Error listing images for cloud: {} in region: {}", cloud.getCloudId(), region,
							imagesTaskExecution.getException());
					return taskExecutionWithResult.mergeLogs(imagesTaskExecution)
							.withException(imagesTaskExecution.getException());
				}
				taskExecutionWithResult.mergeLogs(imagesTaskExecution);
				Set<Image> images = imagesTaskExecution.getResult();
				TaskExecutionWithResult<Set<Hardware>> hardwaresTaskExecution = cloudProvider.getAllHardwares(cloud,
						region, null);
				if (!hardwaresTaskExecution.isSuccess()) {
					logWrapper.error("Error listing hardwares for cloud: {} in region: {}", cloud.getCloudId(), region,
							hardwaresTaskExecution.getException());
					return taskExecutionWithResult.mergeLogs(hardwaresTaskExecution)
							.withException(hardwaresTaskExecution.getException());
				}
				taskExecutionWithResult.mergeLogs(hardwaresTaskExecution);
				Set<Hardware> hardwares = hardwaresTaskExecution.getResult();

				logWrapper.info("Found {} images and {} hardwares in region: {} for cloud: {}", images.size(),
						hardwares.size(), region, cloud.getCloudId());

				// Create a NodeCandidate for each combination of image and hardware
				int candidateCount = 0;
				for (Image image : images) {
					// Filter hardware to match image requirements (based on architecture)
					Set<Hardware> filteredHardwares = hardwares.stream()
							.filter(hardware -> hardware.getArchitecture()
									.equals(image.getOperatingSystem().getOperatingSystemArchitecture()))
							.collect(Collectors.toSet());
					for (Hardware hardware : filteredHardwares) {
						NodeCandidate candidate = new NodeCandidate();

						// Set the node candidate type to IAAS (Infrastructure as a Service)
						candidate.setNodeCandidateType(NodeCandidate.NodeCandidateTypeEnum.IAAS);

						// Set the cloud, location, image, and hardware
						candidate.setCloud(cloud);
						candidate.setLocation(image.getLocation()); // Use location from image
						candidate.setImage(image);
						candidate.setHardware(hardware);

						// Price and other fields can be set later if needed
						candidate.setPrice(null);
						candidate.setPricePerInvocation(null);
						candidate.setMemoryPrice(null);
						candidate.setNodeId(null);
						candidate.setJobIdForEDGE(null);
						taskExecutionWithResult.getResult().add(candidate);
						candidateCount++;
					}
				}

				logWrapper.info(
						"Successfully created {} node candidates from {} images and {} hardwares in region: {} for cloud: {}",
						candidateCount, images.size(), hardwares.size(), region, cloud.getCloudId());
				taskExecutionWithResult.setEnd(new Date());
				taskExecutionWithResult.setSuccess(true);
			} catch (RuntimeException ex) {
				logWrapper.error("RuntimeException while listing node candidates for cloud: {}, region: {}",
						cloud.getCloudId(), region, ex);
				return taskExecutionWithResult.withException(ex);
			} catch (Exception ex) {
				logWrapper.error("Exception while listing node candidates for cloud: {}, region: {}",
						cloud.getCloudId(), region, ex);
				return taskExecutionWithResult.withException(ex);
			}
		}
		return taskExecutionWithResult;
	}

	@Transactional(rollbackFor = {}) // No rollback
	@Async
	public void populateCloudNodeCandidates(Cloud cloud) {

		FetchCloudNodeCandidatesJob job = new FetchCloudNodeCandidatesJob();
		job.setJobType(JobType.FETCH_CLOUD_NODE_CANDIDATES);
		job.setCreatedAt(new Date());
		job.setUpdatedAt(new Date());
		job.setStatus(JobStatus.CREATED);
		job.setTasks(new ArrayList<>());
		job = jobRepository.save(job);
		Task task = new Task();
		task.setJob(job);
		task.setCreatedAt(new Date());
		task.setUpdatedAt(new Date());
		task.setEndedAt(null);
		task.setStatus(JobStatus.CREATED);
		task.setTargetNodeId(null);
		task.setDescription("Fetching cloud node candidates");
		task.setMaxRetries(1);
		task.setExecutions(new LinkedList<Task.TaskExecution>());
		TaskExecutionWithResult<Void> taskExecutionWithResult = new TaskExecutionWithResult<Void>(new Date());
		task.getExecutions().add(taskExecutionWithResult);
		task = taskRepository.save(task);
		cloudNodeCandidateListTask.put(cloud.getCloudId(), false);
		try (LogWrapper logWrapper = taskExecutionWithResult.getLogWrapper(log)) {
			try {

				logWrapper.info("Starting to fetch images and hardwares for cloud: {}", cloud.getCloudId());
				CloudProvider cloudProvider = cloudManager.getCloudProvider(cloud.getCloudProvider());
				TaskExecutionWithResult<Set<String>> listAvailableRegionsResult = cloudProvider
						.listAvailableRegions(cloud);
				if (!listAvailableRegionsResult.isSuccess()) {
					logWrapper.error("Error listing available regions for cloud: {}", cloud.getCloudId(),
							listAvailableRegionsResult.getException());
					cloudNodeCandidateListTask.put(cloud.getCloudId(), false);
					taskExecutionWithResult.mergeLogs(listAvailableRegionsResult)
							.withException(listAvailableRegionsResult.getException());
					return;
				}
				taskExecutionWithResult.mergeLogs(listAvailableRegionsResult);

				Set<String> regions = listAvailableRegionsResult.getResult();
				int regionCount = regions.size();
				int candidateCount = 0;
				regions = Set.of("us-east-1");// TODO: Remove this after testing

				for (String region : regions) {
					// Fetch all images from the cloud provider
					TaskExecutionWithResult<Set<NodeCandidate>> nodeCandidatesTaskExecution = listNodeCandidates(cloud,
							region);
					if (!nodeCandidatesTaskExecution.isSuccess()) {
						logWrapper.error("Error listing node candidates for cloud: {} in region: {}",
								cloud.getCloudId(), region, nodeCandidatesTaskExecution.getException());
						taskExecutionWithResult.mergeLogs(nodeCandidatesTaskExecution)
								.withException(nodeCandidatesTaskExecution.getException());
						return;
					}
					taskExecutionWithResult.mergeLogs(nodeCandidatesTaskExecution);
					Set<NodeCandidate> nodeCandidates = nodeCandidatesTaskExecution.getResult();
					nodeCandidates.forEach(nodeCandidate -> {
						nodeCandidate.setActive(true);
					});

					// Extract unique images and hardwares from node candidates
					Set<Image> uniqueImages = nodeCandidates.stream().map(NodeCandidate::getImage)
							.collect(Collectors.toSet());
					Set<Hardware> uniqueHardwares = nodeCandidates.stream().map(NodeCandidate::getHardware)
							.collect(Collectors.toSet());

					// Save images first (locations will be saved automatically due to
					// CascadeType.ALL)
					logWrapper.info("Saving {} images for cloud: {} in region: {}", uniqueImages.size(),
							cloud.getCloudId(), region);
					imageRepository.saveAll(uniqueImages);

					// Save hardwares second (locations will be saved automatically due to
					// CascadeType.ALL)
					logWrapper.info("Saving {} hardwares for cloud: {} in region: {}", uniqueHardwares.size(),
							cloud.getCloudId(), region);
					hardwareRepository.saveAll(uniqueHardwares);
					entityManager.flush();
					nodeCandidates.forEach(nodeCandidate -> {
						nodeCandidate.setImage(uniqueImages.stream()
								.filter(image -> image.getId().equals(nodeCandidate.getImage().getId())).findFirst()
								.orElse(null));
						nodeCandidate.setHardware(uniqueHardwares.stream()
								.filter(hardware -> hardware.getId().equals(nodeCandidate.getHardware().getId()))
								.findFirst().orElse(null));
					});

					// Finally, save node candidates (now that images and hardwares are persisted)
					nodeCandidateRepository.saveAll(nodeCandidates);
					logWrapper.info("Successfully completed fetching resources for cloud: {} in region: {}",
							cloud.getCloudId(), region);
					candidateCount += nodeCandidates.size();

				}
				logWrapper.info(
						"Successfully completed fetching resources for cloud: {} in all regions ({} regions) with {} node candidates",
						cloud.getCloudId(), regionCount, candidateCount);

				cloudNodeCandidateListTask.put(cloud.getCloudId(), true);
				taskExecutionWithResult.setSuccess(true);
				taskExecutionWithResult.setEnd(new Date());

			} catch (Exception e) {
				logWrapper.error("Unexpected error while fetching resources for cloud {}: {}", cloud.getCloudId(), e);
				cloudNodeCandidateListTask.put(cloud.getCloudId(), false);
				taskExecutionWithResult.setSuccess(false);
				taskExecutionWithResult.setEnd(new Date());
			} finally {
				taskRepository.save(task);
			}
		}
	}

	public Boolean isAnyAsyncCloudPopulationProcessesInProgress(String sessionId) {
		return cloudNodeCandidateListTask.values().stream().parallel().anyMatch(result -> !result);
	}
}
