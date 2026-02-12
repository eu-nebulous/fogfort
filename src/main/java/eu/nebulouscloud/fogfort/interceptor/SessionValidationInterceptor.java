/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import eu.nebulouscloud.fogfort.config.FogFortApplicationConfiguration;
import eu.nebulouscloud.fogfort.service.PAGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionValidationInterceptor implements HandlerInterceptor {

	@Autowired
	private PAGatewayService paGatewayService;
	@Autowired
	private FogFortApplicationConfiguration fogFortApplicationConfiguration;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (fogFortApplicationConfiguration.getSecurityDisabled())
			return true;

		// Skip validation for the connect endpoint (where sessions are created)
		String requestPath = request.getRequestURI();
		if (requestPath != null && requestPath.contains("/pagateway/connect")) {
			return true;
		}

		// Extract sessionId from header
		String sessionId = request.getHeader("sessionid");

		if (sessionId == null || sessionId.isEmpty()) {
			log.warn("Missing sessionId header in request to: {}", requestPath);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\": \"Missing sessionId header\"}");
			return false;
		}

		// Validate session
		if (!paGatewayService.isConnectionActive(sessionId)) {
			log.warn("Invalid sessionId: {} for request to: {}", sessionId, requestPath);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\": \"Invalid or expired sessionId\"}");
			return false;
		}

		return true;
	}
}
