/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.nebulouscloud.fogfort.model.jobs.Job;
import eu.nebulouscloud.fogfort.model.jobs.Task;
import eu.nebulouscloud.fogfort.repository.JobRepository;
import eu.nebulouscloud.fogfort.repository.TaskRepository;
import eu.nebulouscloud.fogfort.util.LogFileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/sal/job")
@Api(tags = "Operations on jobs", consumes = "application/json", produces = "application/json")
@Slf4j
public class JobController {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskRepository taskRepository;

	@GetMapping
	@ApiOperation(value = "Get all jobs with their tasks and executions", response = Job.class, responseContainer = "List")
	@Transactional(readOnly = true)
	public ResponseEntity<List<Job>> getAllJobs() {
		List<Job> jobs = jobRepository.findAll();
		// Force eager loading of tasks and executions
		for (Job job : jobs) {
			if (job.getTasks() != null) {
				job.getTasks().size(); // Force initialization
				for (Task task : job.getTasks()) {
					if (task.getExecutions() != null) {
						task.getExecutions().size(); // Force initialization
					}
				}
			}
		}
		return ResponseEntity.ok(jobs);
	}

	@GetMapping("/{jobId}")
	@ApiOperation(value = "Get a specific job with its tasks and executions", response = Job.class)
	@Transactional(readOnly = true)
	public ResponseEntity<Job> getJob(
			@ApiParam(value = "A job identifier", required = true) @PathVariable(name = "jobId") final String jobId) {
		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
		// Force eager loading of tasks and executions
		if (job.getTasks() != null) {
			job.getTasks().size(); // Force initialization
			for (Task task : job.getTasks()) {
				if (task.getExecutions() != null) {
					task.getExecutions().size(); // Force initialization
				}
			}
		}
		return ResponseEntity.ok(job);
	}

	@GetMapping("/task/{taskId}/execution/{executionIndex}/log")
	@ApiOperation(value = "Get execution output log for a specific task execution", response = String.class)
	@Transactional(readOnly = true)
	public ResponseEntity<String> getExecutionLog(
			@ApiParam(value = "A task identifier", required = true) @PathVariable(name = "taskId") final String taskId,
			@ApiParam(value = "Execution index", required = true) @PathVariable(name = "executionIndex") final int executionIndex) {
		try {
			Task task = taskRepository.findById(taskId)
					.orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

			if (task.getExecutions() == null || executionIndex < 0 || executionIndex >= task.getExecutions().size()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Execution not found");
			}

			Task.TaskExecution execution = task.getExecutions().get(executionIndex);
			if (execution.getOutputLogFileName() == null || execution.getOutputLogFileName().isEmpty()) {
				return ResponseEntity.ok("No output log available");
			}

			try (InputStream logInputStream = LogFileUtils.getInstance()
					.getLogInputStream(execution.getOutputLogFileName())) {
				String logContent = StreamUtils.copyToString(logInputStream, StandardCharsets.UTF_8);
				return ResponseEntity.ok(logContent);
			}
		} catch (IOException e) {
			log.error("Error reading execution log for task {} execution {}", taskId, executionIndex, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error reading log file: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}
}
