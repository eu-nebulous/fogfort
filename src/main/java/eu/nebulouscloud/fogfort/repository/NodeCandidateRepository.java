/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.nebulouscloud.fogfort.model.NodeCandidate;

@Repository
public interface NodeCandidateRepository
		extends JpaRepository<NodeCandidate, String>, JpaSpecificationExecutor<NodeCandidate> {

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM NodeCandidate nc WHERE nc.cloud.id = :cloudId")
	void deleteAllByCloudId(@Param("cloudId") String cloudId);

	@Query("SELECT nc FROM NodeCandidate nc WHERE nc.cloud.id = :cloudId")
	List<NodeCandidate> findByCloudId(@Param("cloudId") String cloudId);

	@Query("SELECT nc FROM NodeCandidate nc WHERE nc.nodeCandidateType = :nodeCandidateType")
	List<NodeCandidate> findByNodeCandidateType(
			@Param("nodeCandidateType") NodeCandidate.NodeCandidateTypeEnum nodeCandidateType);

}
