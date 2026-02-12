/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.model.jobs;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import eu.nebulouscloud.fogfort.model.Node;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Entity
@Table(name = "RUN_SCRIPT_AT_NODE_TASK")
@DiscriminatorValue("RUN_SCRIPT_AT_NODE")
@PrimaryKeyJoinColumn(name = "TASK_ID")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = RunScriptAtNodeTask.class)
public class RunScriptAtNodeTask extends Task {

	@Column(name = "SCRIPT", length = 10000)
	private String script;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "NODE_ID")
	private Node node;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "RUN_SCRIPT_AT_NODE_TASK_OUTPUTS", joinColumns = @JoinColumn(name = "TASK_ID"))
	@MapKeyColumn(name = "OUTPUT_KEY")
	@Column(name = "OUTPUT_VALUE")
	private Map<String, String> outputs = new HashMap<>();

}

