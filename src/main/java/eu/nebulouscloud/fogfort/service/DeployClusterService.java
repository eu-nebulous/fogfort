package eu.nebulouscloud.fogfort.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import eu.nebulouscloud.fogfort.cloud.CloudManager;
import eu.nebulouscloud.fogfort.cloud.CloudProvider;
import eu.nebulouscloud.fogfort.cloud.CloudProvider.NodeCreationDetails;
import eu.nebulouscloud.fogfort.model.Cluster;
import eu.nebulouscloud.fogfort.model.Node;
import eu.nebulouscloud.fogfort.model.SSHConnectionParameters;
import eu.nebulouscloud.fogfort.model.jobs.DeployClusterJob;
import eu.nebulouscloud.fogfort.model.jobs.Job;
import eu.nebulouscloud.fogfort.model.jobs.JobStatus;
import eu.nebulouscloud.fogfort.model.jobs.Task;
import eu.nebulouscloud.fogfort.repository.ClusterRepository;
import eu.nebulouscloud.fogfort.repository.JobRepository;
import eu.nebulouscloud.fogfort.repository.NodeRepository;
import eu.nebulouscloud.fogfort.repository.TaskRepository;
import eu.nebulouscloud.fogfort.util.LogFileUtils;
import eu.nebulouscloud.fogfort.util.ScriptUtils;
import eu.nebulouscloud.fogfort.util.TaskExecutionWithResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service("DeployClusterService")
@Slf4j
public class DeployClusterService {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private CloudManager cloudManager;

	private final ExecutorService executorService = Executors.newCachedThreadPool();
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private ClusterRepository clusterRepository;
	@Autowired
	private NodeRepository nodeRepository;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private NodeScriptRunner nodeScriptRunner;

	@Getter
	@Setter
	private class NodeCreationData {
		public String nodeId;
		public String taskId;
		public TaskExecutionWithResult<NodeCreationDetails> taskExecutionWithResult;
	}

	private CompletableFuture<Void> createClusterNode(String jobId, String nodeId) {

		return CompletableFuture.supplyAsync(() -> {

			final String[] createNodeTaskId = new String[1];
			log.info("Creating cluster node");
			TransactionTemplate template = new TransactionTemplate(transactionManager);
			final Node[] nodeWrapper = new Node[1];
			/*
			 * Transactionally create the node creation task associated to the node creation
			 * job. Retrieve details about the node to be created
			 */
			template.execute(status -> {
				Job createNodeJob = jobRepository.findById(jobId)
						.orElseThrow(() -> new IllegalArgumentException("jobId not found"));
				Cluster cluster = createNodeJob.getCluster();

				if (cluster == null) {
					throw new IllegalArgumentException("Cluster not found in job");
				}
				nodeWrapper[0] = cluster.getNodes().stream().filter(n -> n.getId().equals(nodeId)).findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Node not found"));

				nodeWrapper[0].getNodeCandidate().getCloud().getCloudProvider();
				nodeWrapper[0].getNodeCandidate().getCloud().getCredentials();
				Task createNodeTask = new Task();
				createNodeTask.setJob(createNodeJob);
				createNodeTask.setCreatedAt(new Date());
				createNodeTask.setUpdatedAt(new Date());
				createNodeTask.setEndedAt(null);
				createNodeTask.setStatus(JobStatus.CREATED);
				createNodeTask.setTargetNodeId(null);
				createNodeTask.setDescription("Create node: " + nodeWrapper[0].getName());
				createNodeTask.setMaxRetries(1);
				createNodeTask.setCurrentRetry(0);
				createNodeTask.setExecutions(new ArrayList<>());
				createNodeTask.getExecutions().add(new Task.TaskExecution(new Date()));
				createNodeTask = taskRepository.save(createNodeTask);
				createNodeTaskId[0] = createNodeTask.getId();
				createNodeJob.getTasks().add(createNodeTask);
				return null;
			});
			/*
			 * Create the node on the cloud provider. If it fails, the nodeCreationResult
			 * will contain the error and the node creation task will be updated with the
			 * error.
			 */
			CloudProvider cloudProvider = cloudManager
					.getCloudProvider(nodeWrapper[0].getNodeCandidate().getCloud().getCloudProvider());
			TaskExecutionWithResult<NodeCreationDetails> nodeCreationResult = cloudProvider.createNode(
					nodeWrapper[0].getNodeCandidate().getCloud(), nodeWrapper[0].getNodeCandidate(),
					nodeWrapper[0].getName());
			/*
			 * Transactionally update the node creation task with the result of the node
			 * creation
			 */
			template.execute(status -> {
				Task createNodeTask = taskRepository.findById(createNodeTaskId[0])
						.orElseThrow(() -> new IllegalArgumentException("task not found"));
				Task.TaskExecution taskExecution = createNodeTask.getExecutions().get(0);
				Cluster cluster = createNodeTask.getJob().getCluster();
				Node node = cluster.getNodes().stream().filter(n -> n.getId().equals(nodeId)).findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Node not found"));
				createNodeTask.getExecutions().get(0).updateWith(nodeCreationResult);
				createNodeTask.setStatus(nodeCreationResult.isSuccess() ? JobStatus.COMPLETED : JobStatus.FAILED);
				createNodeTask.setEndedAt(nodeCreationResult.getEnd());
				createNodeTask.setUpdatedAt(nodeCreationResult.getEnd());
				createNodeTask = taskRepository.save(createNodeTask);

				if (nodeCreationResult.isSuccess()) {
					node.setProviderId(nodeCreationResult.getResult().getProviderId());
					node.setStatus(Node.NodeStatus.CREATED);
					node.setNodeUrl(nodeCreationResult.getResult().getNodeUrl());
					node.setSshConnectionParameters(nodeCreationResult.getResult().getSshConnectionParameters());
					node = nodeRepository.save(node);
				} else {
					node.setStatus(Node.NodeStatus.FAILED);
					node = nodeRepository.save(node);
					throw new RuntimeException("Cluster node creation failed");
				}
				return null;
			});
			return null;
		}, executorService);
	}

	/**
	 * Executes a script at a node. Creates a script execution task associated with
	 * the provided jobId and nodeId.
	 * 
	 * @param jobId      The id of the job to which the script execution task is
	 *                   associated
	 * @param nodeId     The id of the node at which the script is to be executed
	 * @param decription The description of the script execution task
	 * @param script     The script to be executed
	 * @return The id of the script execution task
	 */
	private CompletableFuture<String> executeScriptAtNode(String jobId, String nodeId, String decription,
			String script) {
		log.info("Executing script at node: {}", nodeId);
		return CompletableFuture.supplyAsync(() -> {
			/*
			 * Transactionally create the script execution task associated with the provided
			 * jobId and nodeId.
			 */
			TransactionTemplate template = new TransactionTemplate(transactionManager);

			final String[] taskIdW = new String[1];
			final Task.TaskExecution[] taskExecutionW = new Task.TaskExecution[1];
			final SSHConnectionParameters[] sshConnectionParametersW = new SSHConnectionParameters[1];
			template.execute(status -> {
				Job job = jobRepository.findById(jobId)
						.orElseThrow(() -> new IllegalArgumentException("job not found"));

				Node node = nodeRepository.findById(nodeId)
						.orElseThrow(() -> new IllegalArgumentException("node not found"));
				Task runScriptTask = new Task();
				runScriptTask.setJob(job);
				runScriptTask.setCreatedAt(new Date());
				runScriptTask.setUpdatedAt(new Date());
				runScriptTask.setEndedAt(null);
				runScriptTask.setStatus(JobStatus.CREATED);
				runScriptTask.setTargetNodeId(nodeId);
				runScriptTask.setDescription(decription);
				runScriptTask.setMaxRetries(1);
				runScriptTask.setCurrentRetry(0);
				Task.TaskExecution taskExecution = new Task.TaskExecution();
				taskExecution.setStart(new Date());
				taskExecution.setEnd(null);
				taskExecution.setSuccess(false);
				runScriptTask.setExecutions(Collections.singletonList(taskExecution));
				runScriptTask = taskRepository.save(runScriptTask);
				job.getTasks().add(runScriptTask);
				job = jobRepository.save(job);
				taskIdW[0] = runScriptTask.getId();
				taskExecutionW[0] = taskExecution;
				sshConnectionParametersW[0] = node.getSshConnectionParameters();
				return null;
			});

			/*
			 * Execute the script at the node and update the task execution status. Throw an
			 * exception if the script execution fails.
			 */
			nodeScriptRunner.runScript(script, sshConnectionParametersW[0], taskExecutionW[0], null)
					.whenComplete((result, e) -> {
						TransactionTemplate template2 = new TransactionTemplate(transactionManager);
						template.execute((status2) -> {
							Task task = taskRepository.findById(taskIdW[0])
									.orElseThrow(() -> new IllegalArgumentException("task not found"));
							Task.TaskExecution taskExecution = task.getExecutions().get(0);
							taskExecution.updateWith(taskExecutionW[0]);
							task.setStatus(taskExecution.isSuccess() ? JobStatus.COMPLETED : JobStatus.FAILED);
							task.setEndedAt(taskExecution.getEnd());
							task.setUpdatedAt(taskExecution.getEnd());
							task = taskRepository.save(task);
							return null;
						});

						if (!taskExecutionW[0].isSuccess()) {
							if (e != null) {
								throw new RuntimeException("Script execution failed", e);
							}
							throw new RuntimeException("Script execution failed");
						}
					}).join();
			return taskIdW[0];
		});
	}

	/**
	 * Creates the master node for a cluster.
	 * 
	 * @param deployClusterJobId The id of the deploy cluster job
	 * @return A CompletableFuture that completes when the master node is created.
	 *         Throws an exception if the master node creation fails.
	 */
	private CompletableFuture<Void> createMasterNode(String deployClusterJobId) {
		log.info("Creating master node");
		return CompletableFuture.supplyAsync(() -> {
			TransactionTemplate template = new TransactionTemplate(transactionManager);

			final String[] masterNodeIdW = new String[1];
			template.execute(status -> {
				Job deployClusterJob = jobRepository.findById(deployClusterJobId)
						.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
				Cluster cluster = deployClusterJob.getCluster();
				masterNodeIdW[0] = cluster.getMasterNode().getId();
				return null;
			});

			createClusterNode(deployClusterJobId, masterNodeIdW[0]).exceptionally(ex -> {
				log.error("Error creating master node: {}", ex.getMessage());
				throw new RuntimeException("Error creating master node", ex);
			}).join();

			final String[] scriptW = new String[1];
			template.execute(status -> {
				Job deployClusterJob = jobRepository.findById(deployClusterJobId)
						.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
				Cluster cluster = deployClusterJob.getCluster();
				scriptW[0] = ScriptUtils.getMasterInstallScripts(
						cluster.getMasterNode().getSshConnectionParameters().getUsername(), cluster.getEnvVars());
				masterNodeIdW[0] = cluster.getMasterNode().getId();
				return null;
			});

			executeScriptAtNode(deployClusterJobId, masterNodeIdW[0], "Run master install scripts", scriptW[0])
					.exceptionally(ex -> {
						log.error("Error running master install scripts: {}", ex.getMessage());
						// TODO: delete the master node
						throw new RuntimeException("Error running master install scripts", ex);
					}).join();
			return null;
		});
	}

	/**
	 * Creates a kubeadm join token for a node by executing a script at the master
	 * node. The script creates a kubeadm join token and returns it as a script
	 * result.
	 * 
	 * @param deployClusterJobId The id of the deploy cluster job
	 * @param nodeName           The name of the node for which the kubeadm join
	 *                           token is to be created
	 * @return The kubeadm join token Throws an exception if the kubeadm join token
	 *         creation fails.
	 */
	private CompletableFuture<String> createKubeadmJoinCommandForNode(String deployClusterJobId, String nodeName) {
		log.info("Creating kubeadm join token for node: {}", nodeName);
		return CompletableFuture.supplyAsync(() -> {
			TransactionTemplate template = new TransactionTemplate(transactionManager);
			final String[] nodeIdW = new String[1];
			template.execute(status -> {
				Job deployClusterJob = jobRepository.findById(deployClusterJobId)
						.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
				Cluster cluster = deployClusterJob.getCluster();
				nodeIdW[0] = cluster.getMasterNode().getId();
				return null;
			});
			String script = ScriptUtils.getKubeadmJoinCommandScript();

			String taskId = executeScriptAtNode(deployClusterJobId, nodeIdW[0],
					"Create kubeadm join token for node: " + nodeName, script).join();

			@SuppressWarnings("unchecked")
			final Map<String, String>[] resultsW = (Map<String, String>[]) new HashMap[1];
			template.execute(status -> {
				Task task = taskRepository.findById(taskId)
						.orElseThrow(() -> new IllegalArgumentException("task not found"));
				resultsW[0] = nodeScriptRunner.extractScriptResults(task.getExecutions().get(0));
				return null;
			});
			if (resultsW[0] == null || !resultsW[0].containsKey("KUBERNETES_JOIN_TOKEN")) {
				throw new RuntimeException("Kubeadm join token not found");
			}
			return resultsW[0].get("KUBERNETES_JOIN_TOKEN");
		});
	}

	/**
	 * Tries to create a worker node, get a kubeadm join token from the master and
	 * connect the worker to the cluster. If the process fails, the node is deleted
	 * but not exception is thrown.
	 * 
	 * @param deployClusterJobId
	 * @param nodeId
	 * @return
	 */
	private CompletableFuture<Void> createWorkerNode(String deployClusterJobId, String nodeId) {
		log.info("Creating worker node: {}", nodeId);
		return CompletableFuture.runAsync(() -> {
			TransactionTemplate template = new TransactionTemplate(transactionManager);
			try {
				/* Create the worker node */
				createClusterNode(deployClusterJobId, nodeId).join();
				/* Get the kubeadm join token from the master */
				String kubeJoinCommand = createKubeadmJoinCommandForNode(deployClusterJobId, nodeId).join();
				/*
				 * Prepare the worker install script. Add the join command to the script env
				 * vars so the worker can connect to the cluster
				 */
				final String[] scriptW = new String[1];
				template.execute(status -> {
					Job deployClusterJob = jobRepository.findById(deployClusterJobId)
							.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
					Cluster cluster = deployClusterJob.getCluster();					
					Map<String, String> envVars = new HashMap<String, String>();
					envVars.putAll(cluster.getEnvVars());
					envVars.put("variables_kubeCommand", kubeJoinCommand);
					scriptW[0] = ScriptUtils.getWorkerInstallScripts(
							cluster.getMasterNode().getSshConnectionParameters().getUsername(), envVars);
					return null;
				});
				/* Execute the worker install script */
				executeScriptAtNode(deployClusterJobId, nodeId, "Create worker node", scriptW[0]).join();
				return;
			} catch (RuntimeException e) {
				log.error("Error creating worker node: {}", e);
				// TODO: delete the node
			}
		});
	}

	/**
	 * Creates all worker nodes for a cluster.
	 * 
	 * @param deployClusterJobId The id of the deploy cluster job
	 * @return A CompletableFuture that completes when all worker nodes are created.
	 *         Does not throw an exception if any worker node creation fails.
	 */
	private CompletableFuture<Void> createWorkerNodes(String deployClusterJobId) {
		log.info("Creating worker nodes");
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		final List<Pair<String, String>> nodeIdsAndNamesW = new ArrayList<>();
		template.execute(status -> {
			Job deployClusterJob = jobRepository.findById(deployClusterJobId)
					.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
			Cluster cluster = deployClusterJob.getCluster();
			nodeIdsAndNamesW.addAll(
					cluster.getWorkerNodes().stream().map(node -> Pair.of(node.getId(), node.getName())).toList());
			return null;
		});
		return CompletableFuture.allOf(nodeIdsAndNamesW.stream()
				.map(nodeIdAndName -> createWorkerNode(deployClusterJobId, nodeIdAndName.getFirst()))
				.toArray(CompletableFuture[]::new));
	}

	@Async
	public void deployCluster(String clusterId) {
		log.info("Deploying cluster: {}", clusterId);
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		/*
		 * Transactionally create the deploy cluster job associated with the provided
		 * clusterId.
		 */
		final String[] masterNodeIdW = new String[1];
		final String[][] workerNodeIdsW = new String[1][];
		String deployClusterJobId = template.execute(status -> {
			Cluster cluster = clusterRepository.findById(clusterId)
					.orElseThrow(() -> new IllegalArgumentException("Cluster not found"));
			DeployClusterJob deployClusterJob = new DeployClusterJob();
			deployClusterJob.setCluster(cluster);
			deployClusterJob.setCreatedAt(new Date());
			deployClusterJob.setUpdatedAt(new Date());
			deployClusterJob.setEndedAt(null);
			deployClusterJob.setVariables(new HashMap<>());
			deployClusterJob.setStatus(JobStatus.CREATED);
			deployClusterJob.setTasks(new ArrayList<>());
			deployClusterJob = jobRepository.save(deployClusterJob);
			masterNodeIdW[0] = deployClusterJob.getCluster().getMasterNode().getId();
			workerNodeIdsW[0] = deployClusterJob.getCluster().getWorkerNodes().stream().map(Node::getId)
					.toArray(String[]::new);
			return deployClusterJob.getJobId();
		});
		try {
			/* Create the master node and, if successful, create all worker nodes. */
			//createMasterNode(deployClusterJobId).thenAccept(v -> createWorkerNodes(deployClusterJobId)).join();
			createWorkerNodes(deployClusterJobId);
			/* Update the deploy cluster job status to COMPLETED. */
			template.execute(status -> {
				Job deployClusterJob = jobRepository.findById(deployClusterJobId)
						.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
				deployClusterJob.setStatus(JobStatus.COMPLETED);
				deployClusterJob.setEndedAt(new Date());
				deployClusterJob = jobRepository.save(deployClusterJob);
				return null;
			});

		} catch (Exception e) {
			log.error("Error deploying cluster: {}", e.getMessage());
			Job deployClusterJob = jobRepository.findById(deployClusterJobId)
					.orElseThrow(() -> new IllegalArgumentException("DeployClusterJob not found"));
			deployClusterJob.setStatus(JobStatus.FAILED);
			deployClusterJob.setEndedAt(new Date());
			deployClusterJob = jobRepository.save(deployClusterJob);
			// TODO: delete the master node and all worker nodes
		}

	}
}
