/*
 * This Source Code Form is subject to the terms of the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.nebulouscloud.fogfort.config.FogFortApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("PAGatewayService")
public class PAGatewayService {

	@Autowired
	private FogFortApplicationConfiguration fogFortApplicationConfiguration;

	// In-memory storage for active sessions
	// In a production environment, this should be replaced with a proper session
	// management solution
	private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

	/**
	 * Establish connection to server
	 * 
	 * @param username username
	 * @param password password
	 * @return Session ID as a string
	 * @throws Exception if connection fails
	 */
	public String connect(String username, String password) throws Exception {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username cannot be empty");
		}
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password cannot be empty");
		}

		log.info("Connecting to  server with username: {}", username);

		// TODO: Implement actual server connection logic here
		// For now, generating a session ID
		// In a real implementation, this would:
		// 1. Connect to server using the provided credentials
		// 2. Authenticate the user
		// 3. Create and return a valid session ID

		String sessionId = UUID.randomUUID().toString();
		activeSessions.put(sessionId, username);

		log.info("Connection established. Session ID: {}", sessionId);
		return sessionId;
	}

	/**
	 * Disconnect from server
	 * 
	 * @param sessionId The session ID to disconnect
	 * @throws Exception if disconnection fails or session is invalid
	 */
	public void disconnect(String sessionId) throws Exception {
		if (sessionId == null || sessionId.isEmpty()) {
			throw new IllegalArgumentException("Session ID cannot be empty");
		}

		log.info("Disconnecting session: {}", sessionId);

		if (!activeSessions.containsKey(sessionId)) {
			throw new IllegalArgumentException("Invalid session ID: " + sessionId);
		}

		// TODO: Implement actual server disconnection logic here
		// In a real implementation, this would:
		// 1. Notify server to invalidate the session
		// 2. Clean up any resources associated with the session

		activeSessions.remove(sessionId);
		log.info("Session disconnected: {}", sessionId);
	}

	/**
	 * Check if a session is active
	 * 
	 * @param sessionId The session ID to check
	 * @return true if session is active, false otherwise
	 */
	public boolean isConnectionActive(String sessionId) {
		if (fogFortApplicationConfiguration.getSecurityDisabled())
			return true;
		return sessionId != null && activeSessions.containsKey(sessionId);
	}

}
