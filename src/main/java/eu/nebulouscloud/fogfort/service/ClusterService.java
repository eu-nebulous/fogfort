/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.nebulouscloud.fogfort.dto.ClusterApplication;
import eu.nebulouscloud.fogfort.dto.ClusterDefinition;
import eu.nebulouscloud.fogfort.dto.ClusterNodeDefinition;
import eu.nebulouscloud.fogfort.model.Cluster;
import eu.nebulouscloud.fogfort.model.ClusterStatus;
import eu.nebulouscloud.fogfort.model.Node;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.repository.ClusterRepository;
import eu.nebulouscloud.fogfort.repository.NodeCandidateRepository;
import eu.nebulouscloud.fogfort.repository.NodeRepository;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.Validate;

@Slf4j
@Service("ClusterService")
public class ClusterService {

	@Autowired
	private ClusterRepository clusterRepository;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private NodeCandidateRepository nodeCandidateRepository;

	@Autowired
	private PAGatewayService paGatewayService;

	@Autowired
	private DeployClusterService deployClusterService;

	/**
	 * Validates cluster name format (lowercase letters, numbers, and hyphens only)
	 */
	private boolean isValidClusterName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		// Must contain only lowercase letters, numbers, and hyphens
		return name.matches("^[a-z0-9-]+$");
	}

	/**
	 * Validates node name format (must start with letter, only lowercase letters,
	 * digits, and hyphens)
	 */
	private boolean isValidNodeName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		// Must start with a letter and contain only lowercase letters, digits, and
		// hyphens
		return name.matches("^[a-z][a-z0-9-]*$");
	}

	/**
	 * Validates a node definition
	 */
	private void validateNode(ClusterNodeDefinition node) {
		if (!isValidNodeName(node.getNodeName())) {
			throw new IllegalArgumentException("Invalid node name [" + node.getNodeName()
					+ "]. Must start with a letter and contain only lowercase letters, numbers, and hyphens.");
		}

		Optional<NodeCandidate> nc = nodeCandidateRepository.findById(node.getNodeCandidateId());
		if (nc.isEmpty()) {
			throw new IllegalArgumentException("No NodeCandidate found for node [" + node.getNodeName()
					+ "] with candidate ID [" + node.getNodeCandidateId() + "].");
		}
	}

	/**
	 * Define a Kubernetes cluster deployment
	 * 
	 * @param sessionId         A valid session id
	 * @param clusterDefinition Cluster definition
	 * @return true if successful
	 */
	@Transactional
	public boolean defineCluster(String sessionId, ClusterDefinition clusterDefinition) {
		Validate.notNull(clusterDefinition, "The received Cluster definition is empty. Nothing to be defined.");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("defineCluster endpoint is called to define the cluster: {}", clusterDefinition.getName());

		// Check if cluster with this name already exists
		Optional<Cluster> existingCluster = clusterRepository.findByName(clusterDefinition.getName());
		if (existingCluster.isPresent()) {
			throw new IllegalArgumentException(
					"Cluster with name [" + clusterDefinition.getName() + "] already exists.");
		}

		// Validate cluster name
		if (!isValidClusterName(clusterDefinition.getName())) {
			throw new IllegalArgumentException("Invalid cluster name: " + clusterDefinition.getName()
					+ ". Must contain only lowercase letters, numbers, and hyphens.");
		}

		// Validate master node name
		if (!isValidNodeName(clusterDefinition.getMasterNode())) {
			throw new IllegalArgumentException("Invalid master node name [" + clusterDefinition.getMasterNode()
					+ "]. Must start with a letter and contain only lowercase letters, numbers, and hyphens.");
		}

		// Validate that master node exists in nodes list
		boolean masterNodeExists = clusterDefinition.getNodes().stream()
				.anyMatch(node -> node.getNodeName().equals(clusterDefinition.getMasterNode()));
		if (!masterNodeExists) {
			throw new IllegalArgumentException("The master node [" + clusterDefinition.getMasterNode()
					+ "] is not found in the list of defined nodes.");
		}

		// Validate all node names are unique globally
		List<String> allNodeNames = clusterDefinition.getNodes().stream().map(ClusterNodeDefinition::getNodeName)
				.collect(Collectors.toList());

		// Check for duplicates within the cluster
		long uniqueCount = allNodeNames.stream().distinct().count();
		if (uniqueCount != allNodeNames.size()) {
			throw new IllegalArgumentException("Duplicate node names found in cluster definition.");
		}

		// Check for global uniqueness (check existing node definitions)
		for (String nodeName : allNodeNames) {
			Optional<Node> existingNode = nodeRepository.findByName(nodeName);
			if (existingNode.isPresent()) {
				throw new IllegalArgumentException("Node with name [" + nodeName + "] already exists globally.");
			}
		}

		// Validate all nodes
		for (ClusterNodeDefinition nodeDef : clusterDefinition.getNodes()) {
			validateNode(nodeDef);
		}

		// Create and save cluster
		Cluster cluster = new Cluster();
		cluster.setName(clusterDefinition.getName());
		cluster.setMasterNodeName(clusterDefinition.getMasterNode());
		cluster.setStatus(ClusterStatus.DEFINED);
		cluster.setEnvVars(clusterDefinition.getEnvVars());
		cluster.setNodes(new ArrayList<>());

		cluster = clusterRepository.save(cluster);

		// Save node definitions
		for (ClusterNodeDefinition nodeDef : clusterDefinition.getNodes()) {
			Optional<NodeCandidate> nodeCandidate = nodeCandidateRepository.findById(nodeDef.getNodeCandidateId());
			if (nodeCandidate.isEmpty()) {
				throw new IllegalArgumentException("Node candidate not found for id: " + nodeDef.getNodeCandidateId());
			}
			Node modelNode = new Node();
			modelNode.setName(nodeDef.getNodeName());
			modelNode.setNodeCandidate(nodeCandidate.get());
			cluster.getNodes().add(nodeRepository.save(modelNode));
		}

		clusterRepository.save(cluster);

		log.info("Cluster [{}] defined successfully", clusterDefinition.getName());
		return true;
	}

	/**
	 * Deploy a Kubernetes cluster
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @return true if successful
	 */
	@Transactional
	public boolean deployCluster(String sessionId, String clusterName) {
		Validate.notNull(clusterName, "The received clusterName is empty. Nothing to be deployed.");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("deployCluster endpoint is called to deploy the cluster: {}", clusterName);

		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			log.error("No Cluster definition was found for name [{}]! Nothing is deployed!", clusterName);
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		Cluster cluster = clusterOpt.get();

		if (cluster.getStatus() != ClusterStatus.DEFINED) {
			log.warn("Cluster [{}] is not in DEFINED status. Current status: {}", clusterName, cluster.getStatus());
		}

		// Update cluster status to SUBMITTED
		cluster.setStatus(ClusterStatus.SUBMITTED);
		cluster = clusterRepository.save(cluster);
		clusterRepository.flush();

		deployClusterService.deployCluster(cluster.getClusterId());

		log.info("Cluster [{}] deployment initiated", clusterName);
		return true;
	}

	/**
	 * Get cluster information
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @return Cluster with node status information
	 */
	public Cluster getCluster(String sessionId, String clusterName) {
		Validate.notNull(clusterName, "Cluster name cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("getCluster endpoint is called to get the cluster status: {}", clusterName);

		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			log.error("No Cluster was found with name [{}]!", clusterName);
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		Cluster cluster = clusterOpt.get();

		// TODO: Update node statuses
		// This would involve querying for job statuses and updating
		// the cluster node definitions accordingly

		return cluster;
	}

	/**
	 * Deploy and manage applications within a Kubernetes cluster
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @param application Application definition
	 * @return Job ID
	 */
	@Transactional
	public Long manageApplication(String sessionId, String clusterName, ClusterApplication application) {
		Validate.notNull(clusterName, "Cluster name cannot be null");
		Validate.notNull(application, "Application definition cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("manageApplication endpoint is called for cluster [{}] with app [{}]", clusterName,
				application.getAppName());

		// Validate cluster exists
		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		// Validate package manager
		String packageManager = application.getPackageManager();
		if (packageManager == null || (!packageManager.equals("kubectl") && !packageManager.equals("kubevela")
				&& !packageManager.equals("helm"))) {
			throw new IllegalArgumentException("Invalid packageManager. Must be one of: kubectl, kubevela, helm");
		}

		// Validate action
		if (application.getAction() == null || !application.getAction().equals("apply")) {
			throw new IllegalArgumentException("Invalid action. Currently only 'apply' is supported.");
		}

		// TODO: create and submit application deployment
		// workflow
		// This would involve:
		// 1. Creating a  workflow for application deployment
		// 2. Using the specified package manager (kubectl/kubevela/helm)
		// 3. Submitting the workflow to scheduler
		// 4. Returning the submitted job ID

		// For now, return a placeholder job ID
		long jobId = System.currentTimeMillis(); // Placeholder
		log.info("Application [{}] deployment initiated for cluster [{}] with job ID: {}", application.getAppName(),
				clusterName, jobId);

		return jobId;
	}

	/**
	 * Delete a Kubernetes cluster
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @return true if successful
	 */
	@Transactional
	public boolean deleteCluster(String sessionId, String clusterName) {
		Validate.notNull(clusterName, "Cluster name cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("deleteCluster endpoint is called to delete the cluster: {}", clusterName);

		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			log.error("No Cluster was found with name [{}]!", clusterName);
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		Cluster cluster = clusterOpt.get();

		
		if (cluster.getNodes() != null) {
			for (Node node : cluster.getNodes()) {
				nodeRepository.delete(node);
			}
		}
		// Delete cluster
		clusterRepository.delete(cluster);

		log.info("Cluster [{}] deleted successfully", clusterName);
		return true;
	}

	/**
	 * Scale out a Kubernetes cluster by adding worker nodes
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @param newNodes    List of new worker nodes to add
	 * @return Updated cluster
	 */
	@Transactional
	public Cluster scaleOut(String sessionId, String clusterName, List<ClusterNodeDefinition> newNodes) {
		Validate.notNull(clusterName, "Cluster name cannot be null");
		Validate.notNull(newNodes, "New nodes list cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("scaleOut endpoint is called for cluster [{}] to add {} nodes", clusterName, newNodes.size());

		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		Cluster cluster = clusterOpt.get();

		// Validate new nodes
		for (ClusterNodeDefinition nodeDef : newNodes) {
			validateNode(nodeDef);

			// Check if node name already exists
			Optional<Node> existingNode = nodeRepository.findByName(nodeDef.getNodeName());
			if (existingNode.isPresent()) {
				throw new IllegalArgumentException("Node with name [" + nodeDef.getNodeName() + "] already exists.");
			}
		}

		// Update cluster status
		cluster.setStatus(ClusterStatus.SCALING);
		clusterRepository.save(cluster);

		// Save new node definitions
		for (ClusterNodeDefinition nodeDef : newNodes) {
			Optional<NodeCandidate> nodeCandidate = nodeCandidateRepository.findById(nodeDef.getNodeCandidateId());
			if (nodeCandidate.isEmpty()) {
				throw new IllegalArgumentException("Node candidate not found for id: " + nodeDef.getNodeCandidateId());
			}
			Node modelNode = new Node();
			modelNode.setName(nodeDef.getNodeName());
			modelNode.setNodeCandidate(nodeCandidate.get());
			cluster.getNodes().add(modelNode);
		}
		

		log.info("Scale out initiated for cluster [{}] with {} new nodes", clusterName, newNodes.size());
		return cluster;
	}

	/**
	 * Scale in a Kubernetes cluster by removing worker nodes
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @param nodeNames   List of worker node names to remove
	 * @return Updated cluster
	 */
	@Transactional
	public Cluster scaleIn(String sessionId, String clusterName, List<String> nodeNames) {
		Validate.notNull(clusterName, "Cluster name cannot be null");
		Validate.notNull(nodeNames, "Node names list cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("scaleIn endpoint is called for cluster [{}] to remove {} nodes", clusterName, nodeNames.size());

		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		Cluster cluster = clusterOpt.get();

		// Validate that nodes exist and are not master node
		for (String nodeName : nodeNames) {
			if (nodeName.equals(cluster.getMasterNodeName())) {
				throw new IllegalArgumentException("Cannot remove master node [" + nodeName + "].");
			}
			Node node = cluster.getNodes().stream().filter(n -> n.getName().equals(nodeName)).findFirst().orElseThrow(
					() -> new IllegalArgumentException("Node with name [" + nodeName + "] not found in cluster."));
			nodeRepository.delete(node); // TODO really need to delete the node?
		}

		// Update cluster status
		cluster.setStatus(ClusterStatus.SCALING);
		clusterRepository.save(cluster);

		log.info("Scale in initiated for cluster [{}] to remove {} nodes", clusterName, nodeNames.size());
		return cluster;
	}

	/**
	 * Label nodes in a Kubernetes cluster
	 * 
	 * @param sessionId   A valid session id
	 * @param clusterName Cluster name
	 * @param nodeLabels  Map of node names to labels (key=value format)
	 * @return Job ID
	 */
	@Transactional
	public Long labelNode(String sessionId, String clusterName, Map<String, String> nodeLabels) {
		Validate.notNull(clusterName, "Cluster name cannot be null");
		Validate.notNull(nodeLabels, "Node labels cannot be null");

		if (!paGatewayService.isConnectionActive(sessionId)) {
			throw new IllegalArgumentException("Invalid or inactive session ID");
		}

		log.info("labelNode endpoint is called for cluster [{}] to label {} nodes", clusterName, nodeLabels.size());

		// Validate cluster exists
		Optional<Cluster> clusterOpt = clusterRepository.findByName(clusterName);
		if (clusterOpt.isEmpty()) {
			throw new IllegalArgumentException("Cluster with name [" + clusterName + "] not found.");
		}

		// Validate that all nodes exist in the cluster
		for (String nodeName : nodeLabels.keySet()) {
			Optional<Node> nodeDef = nodeRepository.findByName(nodeName);
			if (nodeDef.isEmpty()) {
				throw new IllegalArgumentException("Node with name [" + nodeName + "] not found in cluster.");
			}
		}

		
		long jobId = System.currentTimeMillis(); // Placeholder
		log.info("Node labeling initiated for cluster [{}] with job ID: {}", clusterName, jobId);

		return jobId;
	}
}
