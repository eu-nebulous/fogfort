/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.model.User;
import eu.nebulouscloud.fogfort.service.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * Implement CRUD methods for REST service
 */

@RestController
@RequestMapping(value = "/sal/users/")
@Slf4j
public class UserController {

	@Autowired
	private UserService userService;

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<Collection<User>> listAllUsers() {
		Collection<User> users = userService.findAllUsers();
		if (users.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(users, HttpStatus.OK);
	}

	@RequestMapping(value = "{name}", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<User> getUser(@PathVariable("name") String name) {
		log.debug("Fetching User with name " + name);
		return userService.findByName(name).map(user -> new ResponseEntity<>(user, HttpStatus.OK))
				.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<User> createUser(@RequestBody User user) {
		log.debug("Creating User " + user.getName());
		return userService.findByName(user.getName())
				.map(userFound -> new ResponseEntity<>(userFound, HttpStatus.CONFLICT)).orElseGet(() -> {
					userService.saveUser(user);
					return new ResponseEntity<>(user, HttpStatus.CREATED);
				});
	}

	@RequestMapping(value = "{name}", method = RequestMethod.PUT)
	public ResponseEntity<User> updateUser(@PathVariable("name") String name, @RequestBody User user) {
		log.debug("Updating User " + name);

		return userService.findByName(name).map(userFound -> {
			userService.updateUser(user);
			return new ResponseEntity<>(user, HttpStatus.OK);
		}).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

	}

	@RequestMapping(value = "{name}", method = RequestMethod.DELETE)
	public ResponseEntity<User> deleteUser(@PathVariable("name") String name) {
		log.debug("Fetching & Deleting User with name " + name);

		return userService.findByName(name).map(userFound -> {
			userService.deleteUserByName(name);
			return new ResponseEntity<>(userFound, HttpStatus.OK);
		}).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

	}

	@RequestMapping(method = RequestMethod.DELETE)
	public ResponseEntity<User> deleteAllUsers() {
		log.debug("Deleting All Users");
		userService.deleteAllUsers();
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
