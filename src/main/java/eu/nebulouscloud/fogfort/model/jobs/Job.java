/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model.jobs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;

import eu.nebulouscloud.fogfort.model.Cluster;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Entity
@Table(name = "JOB")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "JOB_TYPE", discriminatorType = DiscriminatorType.STRING)
public class Job implements Serializable {
	@Id
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Column(name = "JOB_ID")
	private String jobId;

	@Column(name = "CREATED_AT")
	private Date createdAt;
	@Column(name = "UPDATED_AT")
	private Date updatedAt;
	@Column(name = "ENDED_AT")
	private Date endedAt;

	@Column(name = "STATUS")
	@Enumerated(EnumType.STRING)
	private JobStatus status;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, optional = true)
	private Cluster cluster;

	@Column(name = "VARIABLES")
	@ElementCollection(targetClass = String.class)
	private Map<String, String> variables;

	@Column(name = "JOB_TYPE", insertable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	private JobType jobType;

	@OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Task> tasks;

}
