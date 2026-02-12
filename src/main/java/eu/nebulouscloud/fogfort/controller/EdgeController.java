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
import org.springframework.web.bind.annotation.*;

import eu.nebulouscloud.fogfort.dto.EdgeDefinition;
import eu.nebulouscloud.fogfort.dto.NodeCandidate;
import eu.nebulouscloud.fogfort.dto.NotConnectedException;
import eu.nebulouscloud.fogfort.service.EdgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = "/sal/edge")
@Api(tags = "Operations on Edge", consumes = "application/json", produces = "application/json")
public class EdgeController {

	@Autowired
	private EdgeService edgeService;

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	@ApiOperation(value = "Register new Edge nodes passed as EdgeDefinition object", response = NodeCandidate.class)
	public ResponseEntity<NodeCandidate> registerNewEdgeNode(
			@ApiParam(value = "Authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "object of class EdgeDefinition that contains the details of the nodes to be registered.", required = true) @RequestBody final EdgeDefinition edgeNodeDefinition) {
		return ResponseEntity.ok(edgeService.registerNewEdgeNode(sessionId, edgeNodeDefinition));
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ApiOperation(value = "Get all registered edges", response = NodeCandidate.class, responseContainer = "List")
	public ResponseEntity<List<NodeCandidate>> getEdgeNodes(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId) {
		return ResponseEntity.ok(edgeService.getEdgeNodes(sessionId));
	}

	@RequestMapping(value = "/{edgeId}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove Edge nodes", response = Boolean.class)
	public ResponseEntity<Boolean> deleteEdgeNode(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "The id of the node to be removed", required = true) @PathVariable(name="edgeId") final String edgeId) {
		return ResponseEntity.ok(edgeService.deleteEdgeNode(sessionId, edgeId));
	}
}
