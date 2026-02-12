/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.dto.Requirement;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.service.NodeCandidateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = "/sal/nodecandidates")
@Api(tags = "SAL Operations", consumes = "application/json", produces = "application/json")
public class NodeCandidateController {

	@Autowired
	private NodeCandidateService nodeCandidateService;

	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(value = "Find node candidates", response = NodeCandidate.class, responseContainer = "List")
	public ResponseEntity<List<NodeCandidate>> findNodeCandidates(
			@ApiParam(value = "authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId,
			@ApiParam(value = "List of requirements (NodeTypeRequirement and AttributeRequirement)", required = true) @RequestBody final List<Requirement> requirements) {
		return ResponseEntity.ok(nodeCandidateService.findNodeCandidates(sessionId, requirements));
	}
}
