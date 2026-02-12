package eu.nebulouscloud.fogfort.model.jobs;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Entity
@DiscriminatorValue("FETCH_CLOUD_NODE_CANDIDATES")
public class FetchCloudNodeCandidatesJob extends Job {
}
