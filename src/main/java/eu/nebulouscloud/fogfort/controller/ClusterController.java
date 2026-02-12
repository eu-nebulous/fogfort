/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.dto.ClusterApplication;
import eu.nebulouscloud.fogfort.dto.ClusterDefinition;
import eu.nebulouscloud.fogfort.dto.ClusterNodeDefinition;
import eu.nebulouscloud.fogfort.model.Cluster;
import eu.nebulouscloud.fogfort.repository.ClusterRepository;
import eu.nebulouscloud.fogfort.service.ClusterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/sal/cluster")
@Api(tags = "Operations on Kubernetes cluster", consumes = "application/json", produces = "application/json")
@Slf4j
public class ClusterController {

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private ClusterRepository clusterRepository;

	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(value = "Define a Kubernetes cluster deployment", notes = "This endpoint is used to define a Kubernetes cluster deployment. Script templates for configuring the deployment workflow are available. They can be modified to incorporate user-defined Kubernetes installation scripts, and for public clouds they need to have installed and set the network component.", response = Boolean.class)
	public ResponseEntity<Boolean> defineCluster(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "A cluster definition following ClusterDefinition format", required = true) @RequestBody final ClusterDefinition clusterDefinition) {
		if (clusterDefinition == null) {
			throw new IllegalArgumentException("Cluster definitions are empty");
		}
		return ResponseEntity.ok(clusterService.defineCluster(sessionId, clusterDefinition));
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.POST)
	@ApiOperation(value = "Deploy a Kubernetes cluster", notes = "This endpoint enables users to configure and deploy a Kubernetes cluster. The deployment process involves cluster definition integration, workflow execution, and resource monitoring.", response = Boolean.class)
	public ResponseEntity<Boolean> deployCluster(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName) {
		return ResponseEntity.ok(clusterService.deployCluster(sessionId, clusterName));
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.GET)
	@ApiOperation(value = "Get cluster information", notes = "This endpoint retrieves detailed information about the Kubernetes cluster deployment. It provides real-time status updates on the deployment progress for each individual node, as well as the overall cluster.", response = Cluster.class)
	public ResponseEntity<Cluster> getCluster(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName) {
		return ResponseEntity.ok(clusterService.getCluster(sessionId, clusterName));
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ApiOperation(value = "Get all clusters with their nodes", response = Cluster.class, responseContainer = "List")
	@Transactional(readOnly = true)
	public ResponseEntity<List<Cluster>> getAllClusters() {
		List<Cluster> clusters = clusterRepository.findAll();
		// Force eager loading of nodes
		for (Cluster cluster : clusters) {
			if (cluster.getNodes() != null) {
				cluster.getNodes().size(); // Force initialization
			}
		}
		return ResponseEntity.ok(clusters);
	}

	@RequestMapping(value = "/{clusterName}/app", method = RequestMethod.POST)
	@ApiOperation(value = "Deploy and manage applications within a Kubernetes cluster", notes = "This endpoint is used to deploy and manage applications within a specific Kubernetes cluster, utilizing kubectl, KubeVela, or Helm. Upon initiating a deployment, the endpoint generates an application deployment workflow within the designated cluster.", response = Long.class)
	public ResponseEntity<Long> manageApplication(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName,
			@ApiParam(value = "Application definition with appFile, packageManager, appName, action, and flags", required = true) @RequestBody final ClusterApplication application) {
		if (application == null) {
			throw new IllegalArgumentException("Applications are empty");
		}
		return ResponseEntity.ok(clusterService.manageApplication(sessionId, clusterName, application));
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Delete a Kubernetes cluster", notes = "This endpoint allows users to delete an existing Kubernetes cluster deployment. It removes all resources associated with the specified cluster, including nodes, network configurations, and any deployed applications.", response = Boolean.class)
	public ResponseEntity<Boolean> deleteCluster(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName) {
		return ResponseEntity.ok(clusterService.deleteCluster(sessionId, clusterName));
	}

	@RequestMapping(value = "/{clusterName}/scaleout", method = RequestMethod.POST)
	@ApiOperation(value = "Scale out a Kubernetes cluster", notes = "This endpoint allows users to dynamically expand their Kubernetes cluster by adding new worker nodes. This scaling operation is based on existing worker node definitions and is critical when increasing the cluster's capacity to support more replicas for applications.", response = Cluster.class)
	public ResponseEntity<Cluster> scaleOut(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName,
			@ApiParam(value = "List of new worker nodes to add", required = true) @RequestBody final List<ClusterNodeDefinition> newNodes) {
		return ResponseEntity.ok(clusterService.scaleOut(sessionId, clusterName, newNodes));
	}

	@RequestMapping(value = "/{clusterName}/scalein", method = RequestMethod.POST)
	@ApiOperation(value = "Scale in a Kubernetes cluster", notes = "This endpoint allows users to remove specific worker nodes from a Kubernetes cluster. This operation is essential for efficiently managing cluster resources, especially when scaling down applications or reconfiguring the cluster architecture.", response = Cluster.class)
	public ResponseEntity<Cluster> scaleIn(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName,
			@ApiParam(value = "List of worker node names to remove", required = true) @RequestBody final List<String> nodeNames) {
		return ResponseEntity.ok(clusterService.scaleIn(sessionId, clusterName, nodeNames));
	}

	@RequestMapping(value = "/{clusterName}/label", method = RequestMethod.POST)
	@ApiOperation(value = "Label nodes in a Kubernetes cluster", notes = "This endpoint allows users to manage node labels within a Kubernetes cluster by adding, modifying, or removing labels. Labels are key-value pairs that categorize nodes, making it easier to target specific nodes for application deployment, scaling, or management tasks.", response = Long.class)
	public ResponseEntity<Long> labelNode(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "Cluster name", required = true) @PathVariable(name = "clusterName") final String clusterName,
			@ApiParam(value = "Map of node names to labels (key=value format)", required = true) @RequestBody final List<Map<String, String>> nodeLabels) {
		if (nodeLabels == null || nodeLabels.isEmpty()) {
			throw new IllegalArgumentException("Node labels are empty");
		}
		return ResponseEntity.ok(clusterService.labelNode(sessionId, clusterName, nodeLabels.get(0)));
	}

}
