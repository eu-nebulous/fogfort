/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.net.URLConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for serving static GUI resources.
 * Provides endpoints for serving static files (HTML, CSS, JavaScript, images, etc.)
 * from the classpath static directory. The default index file is jobs.html.
 * 
 * @author FogFort Team
 */
@RestController
@RequestMapping(value = "/gui")
@Slf4j
public class GUIController {

	@Autowired
	private ResourceLoader resourceLoader;

	@RequestMapping(value = "/{filename:.+}", method = RequestMethod.GET)
	public ResponseEntity<Resource> serveStaticFile(@PathVariable String filename) {
		try {
			Resource resource = resourceLoader.getResource("classpath:static/" + filename);
			
			if (!resource.exists() || !resource.isReadable()) {
				log.warn("Static resource not found: {}", filename);
				return ResponseEntity.notFound().build();
			}

			// Determine content type
			String contentType = determineContentType(filename);
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(resource);
		} catch (Exception e) {
			log.error("Error serving static resource: {}", filename, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	String indexFilename = "jobs.html";
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Resource> serveIndex() {
		return serveStaticFile(indexFilename);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ResponseEntity<Resource> serveIndex2() {
		return serveStaticFile(indexFilename);
	}

	private String determineContentType(String filename) {
		String contentType = URLConnection.guessContentTypeFromName(filename);
		if (contentType == null) {
			// Fallback to common content types based on file extension
			String lowerFilename = filename.toLowerCase();
			if (lowerFilename.endsWith(".html")) {
				contentType = "text/html";
			} else if (lowerFilename.endsWith(".css")) {
				contentType = "text/css";
			} else if (lowerFilename.endsWith(".js")) {
				contentType = "application/javascript";
			} else if (lowerFilename.endsWith(".json")) {
				contentType = "application/json";
			} else if (lowerFilename.endsWith(".png")) {
				contentType = "image/png";
			} else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
				contentType = "image/jpeg";
			} else if (lowerFilename.endsWith(".gif")) {
				contentType = "image/gif";
			} else if (lowerFilename.endsWith(".svg")) {
				contentType = "image/svg+xml";
			} else if (lowerFilename.endsWith(".ico")) {
				contentType = "image/x-icon";
			} else {
				contentType = "application/octet-stream";
			}
		}
		return contentType;
	}
}

