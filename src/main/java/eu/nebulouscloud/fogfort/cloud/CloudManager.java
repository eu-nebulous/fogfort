
package eu.nebulouscloud.fogfort.cloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.nebulouscloud.fogfort.dto.CloudProviderType;

@Service
public class CloudManager {

	@Autowired
	private AWSCloudProvider awsCloudProvider;

	public CloudProvider getCloudProvider(CloudProviderType type) {
		switch (type) {
		case AWS_EC2:
			return awsCloudProvider;
		default:
			throw new RuntimeException("Not implemented yet");
		}
	}

}
