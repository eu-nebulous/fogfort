/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import eu.nebulouscloud.fogfort.model.Cloud;

@Repository
public interface CloudRepository extends JpaRepository<Cloud, String> {

	@Transactional(readOnly = true)
	@Query(value = "SELECT id FROM Cloud WHERE id NOT IN (SELECT cloud.id FROM NodeCandidate GROUP BY cloud.id)")
	List<String> getOrphanCloudIds();

	@Modifying(clearAutomatically = true)
	@Query(value = "DELETE FROM Cloud WHERE id NOT IN (SELECT cloud.id FROM NodeCandidate GROUP BY cloud.id)")
	void deleteOrphanCloudIds();
}
