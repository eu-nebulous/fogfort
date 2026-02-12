/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.dto.ConnectRequest;
import eu.nebulouscloud.fogfort.service.PAGatewayService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/sal/pagateway")
@Api(tags = "Gateway Operations", produces = "text/plain")
public class PAGatewayController {

	@Autowired
	private PAGatewayService paGatewayService;

	@RequestMapping(value = "/connect", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Establish connection to  server (JSON)", notes = "Returns a session ID that should be used as a header for all other endpoints. If HTTP 500 error is returned with body saying that NonConnectedException occur, it means that connection to   server was lost and is needed to call Connect endpoint again and use the new session ID.", response = String.class)
	public ResponseEntity<String> connectJson(
			@ApiParam(value = "  username and password", required = true) @RequestBody final ConnectRequest connectRequest) {
		try {
			String sessionId = paGatewayService.connect(connectRequest.getUsername(), connectRequest.getPassword());
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(sessionId);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
					.body("Error: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error connecting to   server", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
					.body("Error: " + e.getMessage());
		}
	}

	@RequestMapping(value = "/connect", method = RequestMethod.POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE,
			MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	@ApiOperation(value = "Establish connection to  server (Form Data)", notes = "Returns a session ID that should be used as a header for all other endpoints. If HTTP 500 error is returned with body saying that NonConnectedException occur, it means that connection to   server was lost and is needed to call Connect endpoint again and use the new session ID.", response = String.class)
	public ResponseEntity<String> connectForm(
			@ApiParam(value = "Username", required = true) @RequestParam(value = "username") final String username,
			@ApiParam(value = "Password", required = true) @RequestParam(value = "password") final String password) {
		try {
			String sessionId = paGatewayService.connect(username, password);
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(sessionId);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
					.body("Error: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error connecting to   server", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
					.body("Error: " + e.getMessage());
		}
	}

	@RequestMapping(value = "/disconnect", method = RequestMethod.POST)
	@ApiOperation(value = "Disconnect from   server")
	public ResponseEntity<Void> disconnect(
			@ApiParam(value = "  authentication session id", required = true) @RequestHeader(value = "sessionid") final String sessionId) {
		try {
			paGatewayService.disconnect(sessionId);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (Exception e) {
			log.error("Error disconnecting from   server", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
