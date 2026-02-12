/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model.jobs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Entity
@Table(name = "TASK")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "TASK_TYPE", discriminatorType = DiscriminatorType.STRING)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Task.class)
public class Task implements Serializable {

	@Getter
	@Setter
	@NoArgsConstructor
	@Embeddable
	@ToString(callSuper = true)
	public static class TaskExecution implements Serializable {
		protected Date start;
		protected Date end;
		protected String outputLogFileName = UUID.randomUUID().toString() + ".log";
		protected boolean success;

		public boolean terminated() {
			return this.end != null;
		}

		public TaskExecution(Date start) {
			this.start = start;
		}

		public void updateWith(TaskExecution other) {
			if (this.start == null)
				this.start = other.start;
			if (other.start != null && other.start.before(this.start))
				this.start = other.start;
			this.end = other.end;
			this.outputLogFileName = other.outputLogFileName;
			this.success = other.success;
		}

	}

	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Column(name = "TASK_ID")
	private String id;
	private String targetNodeId;

	private String description;
	private int maxRetries;
	private int currentRetry;
	private Date createdAt;
	private Date updatedAt;
	private Date endedAt;
	private JobStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "JOB_ID")
	@JsonIgnore
	private Job job;

	@ElementCollection
	private List<TaskExecution> executions;

}
