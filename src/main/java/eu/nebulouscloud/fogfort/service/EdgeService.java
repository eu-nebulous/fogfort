/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.nebulouscloud.fogfort.config.ServiceConfiguration;
import eu.nebulouscloud.fogfort.dto.EdgeDefinition;
import eu.nebulouscloud.fogfort.dto.NodeCandidate;
import eu.nebulouscloud.fogfort.model.Node;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("EdgeService")
public class EdgeService {

	@Autowired
	private ServiceConfiguration serviceConfiguration;

	/**
	 * Register new Edge nodes passed as EdgeDefinition object
	 *
	 * @param sessionId          A valid session id
	 * @param edgeNodeDefinition objects of class ByonDefinition that contains the
	 *                           detials of the nodes to be registered.
	 * @return newEdgeNode EdgeNode object that contains information about the
	 *         registered Node
	 */
	public NodeCandidate registerNewEdgeNode(String sessionId, EdgeDefinition edgeNodeDefinition) {

		throw new UnsupportedOperationException("Not implemented");
		/*
		 * Validate.notNull(edgeNodeDefinition,
		 * "The received EDGE node definition is empty. Nothing to be registered.");
		 * log.
		 * info("registerNewEdgeNode endpoint is called, Registering a new EDGE definition ..."
		 * ); EdgeNode newEdgeNode = new EdgeNode(); String jobId; if
		 * (edgeNodeDefinition.getJobId() == null ||
		 * edgeNodeDefinition.getJobId().isEmpty()) { jobId = EdgeDefinition.ANY_JOB_ID;
		 * } else { jobId = edgeNodeDefinition.getJobId(); }
		 * newEdgeNode.setName(edgeNodeDefinition.getName());
		 * newEdgeNode.setLoginCredential(edgeNodeDefinition.getLoginCredential());
		 * newEdgeNode.setIpAddresses(edgeNodeDefinition.getIpAddresses());
		 * newEdgeNode.setNodeProperties(edgeNodeDefinition.getNodeProperties());
		 * newEdgeNode.setJobId(jobId);
		 * newEdgeNode.setPort(edgeNodeDefinition.getPort());
		 * newEdgeNode.setSystemArch(edgeNodeDefinition.getSystemArch());
		 * newEdgeNode.setScriptURL(edgeNodeDefinition.getScriptURL());
		 * newEdgeNode.setJarURL(edgeNodeDefinition.getJarURL());
		 * 
		 * NodeCandidate edgeNC =
		 * ByonUtils.createNodeCandidate(edgeNodeDefinition.getNodeProperties(), jobId,
		 * "edge", newEdgeNode.getId(), edgeNodeDefinition.getName());
		 * newEdgeNode.setNodeCandidate(edgeNC);
		 * 
		 * repositoryService.saveEdgeNode(newEdgeNode); repositoryService.flush();
		 * log.info("EDGE node registered.");
		 * 
		 * return newEdgeNode;
		 */
		//return null;
		/*
		 * TODO: Avoid duplicate nodes in the database
		 */
	}

	/**
	 * Return the List of registered EDGE nodes
	 * 
	 * @param sessionId A valid session id
	 * @return List of EdgeNode objects that contains information about the
	 *         registered Nodes
	 */
	public List<NodeCandidate> getEdgeNodes(String sessionId) {
		throw new UnsupportedOperationException("Not implemented");
		/*
		 * List<EdgeNode> filteredEdgeNodes = new LinkedList<>(); List<EdgeNode>
		 * listEdgeNodes = repositoryService.listEdgeNodes(); if (jobId.equals("0")) {
		 * return listEdgeNodes; } else { for (EdgeNode edgeNode : listEdgeNodes) { if
		 * (jobId.equals(edgeNode.getJobId())) { filteredEdgeNodes.add(edgeNode); } }
		 * return filteredEdgeNodes; }
		 */
		//return null;
		/*
		 * TODO: Add Logging info
		 */
	}

	/**
	 * Delete Edge nodes
	 * 
	 * @param sessionId A valid session id
	 * @param edgeId    the id of the node to be removed
	 * @return true if the deletion was done with no errors, false otherwise
	 */
	public boolean deleteEdgeNode(String sessionId, String edgeId) {
		throw new UnsupportedOperationException("Not implemented");
		/*
		 * EdgeNode edgeNode = repositoryService.getEdgeNode(edgeId);
		 * 
		 * if (edgeNode == null) {
		 * log.error("The passed EDGE ID is not Found in the database"); throw new
		 * IllegalArgumentException("The passed EDGE ID \"" + edgeId +
		 * "\" is not Found in the database"); }
		 * 
		 * handlePACloudDeletion(edgeNode);
		 * 
		 * repositoryService.deleteEdgeNode(edgeNode);
		 */
		//return true;
	}


}
