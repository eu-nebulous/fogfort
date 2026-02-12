package eu.nebulouscloud.fogfort.cloud;

import java.util.Set;

import eu.nebulouscloud.fogfort.dto.CloudProviderType;
import eu.nebulouscloud.fogfort.model.Cloud;
import eu.nebulouscloud.fogfort.model.Hardware;
import eu.nebulouscloud.fogfort.model.Image;
import eu.nebulouscloud.fogfort.model.Node;
import eu.nebulouscloud.fogfort.model.NodeCandidate;
import eu.nebulouscloud.fogfort.model.OperatingSystemArchitecture;
import eu.nebulouscloud.fogfort.model.SSHConnectionParameters;
import eu.nebulouscloud.fogfort.util.TaskExecutionWithResult;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Interface for cloud provider implementations that manage cloud infrastructure operations.
 * This interface defines the contract for interacting with various cloud providers (e.g., AWS, Azure, GCP)
 * to perform operations such as creating and deleting nodes, listing available regions, images, and hardware profiles.
 * 
 * Implementations of this interface handle the provider-specific logic for managing cloud resources
 * and provide a unified API for cloud operations.
 */
public interface CloudProvider {

	/**
	 * Contains the details of a newly created node, including connection information
	 * and provider-specific identifiers.
	 */
	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	@ToString
	@EqualsAndHashCode
	public class NodeCreationDetails {
		/** SSH connection parameters for accessing the newly created node */
		public SSHConnectionParameters sshConnectionParameters;
		/** The provider-specific unique identifier for the created node */
		public String providerId;
		/** The URL or endpoint for accessing the node */
		public String nodeUrl;
	}

	/**
	 * Returns the type of cloud provider that this implementation manages.
	 * This method identifies which cloud platform (e.g., AWS, Azure, GCP) the provider
	 * implementation is designed to work with.
	 * 
	 * @return The cloud provider type enum value indicating which cloud platform this provider manages
	 */
	public CloudProviderType getType();

	/**
	 * Retrieves all available regions supported by the cloud provider.
	 * This method queries the cloud provider to identify all geographic regions
	 * where resources can be provisioned.
	 * 
	 * @param cloud The cloud configuration containing provider-specific credentials and settings
	 * @return A TaskExecutionWithResult containing a set of region identifiers (e.g., "us-east-1", "eu-west-1")
	 *         that are available for resource provisioning
	 */
	public TaskExecutionWithResult<Set<String>> listAvailableRegions(Cloud cloud);

	/**
	 * Creates a new compute node in the specified cloud provider.
	 * This method provisions a new virtual machine or instance based on the provided
	 * node candidate specifications, including hardware profile, image, and region.
	 * 
	 * @param cloud         The cloud configuration containing provider credentials and settings
	 *                      where the node will be created
	 * @param nodeCandidate The specification of the node to be created, including hardware profile,
	 *                      operating system image, region, and other configuration details
	 * @param nodeName      The name to assign to the newly created node
	 * @return A TaskExecutionWithResult containing NodeCreationDetails with SSH connection parameters,
	 *         provider-specific node ID, and node URL upon successful creation
	 */
	public TaskExecutionWithResult<NodeCreationDetails> createNode(Cloud cloud, NodeCandidate nodeCandidate,
			String nodeName);

	/**
	 * Terminates and removes a node from the cloud provider.
	 * This method permanently deletes the specified compute node and releases all associated resources.
	 * The operation is irreversible once completed.
	 * 
	 * @param cloud The cloud configuration containing provider credentials and settings
	 *              where the node is currently hosted
	 * @param node  The node to be terminated and removed from the cloud provider
	 * @return A TaskExecutionWithResult with Void result indicating successful deletion when completed
	 */
	public TaskExecutionWithResult<Void> deleteNode(Cloud cloud, Node node);

	/**
	 * Retrieves all available operating system images that can be used to boot nodes.
	 * This method lists all pre-configured system images (e.g., Ubuntu, CentOS, Windows)
	 * available in the specified region for creating new nodes.
	 * 
	 * @param cloud  The cloud configuration containing provider credentials and settings
	 * @param region The region identifier (e.g., "us-east-1") from which to retrieve available images
	 * @return A TaskExecutionWithResult containing a set of Image objects representing all bootable
	 *         system images available in the specified region
	 */
	public TaskExecutionWithResult<Set<Image>> getAllImages(Cloud cloud, String region);

	/**
	 * Retrieves all available hardware profiles (instance types) that can be used to provision nodes.
	 * This method lists all compute instance types (e.g., t2.micro, m5.large) available in the
	 * specified region that match the given operating system architecture.
	 * 
	 * @param cloud       The cloud configuration containing provider credentials and settings
	 * @param region      The region identifier (e.g., "us-east-1") from which to retrieve hardware profiles
	 * @param architecture The operating system architecture (e.g., x86_64, ARM64) for which to filter
	 *                    available hardware profiles
	 * @return A TaskExecutionWithResult containing a set of Hardware objects representing all available
	 *         instance types matching the specified architecture in the given region
	 */
	public TaskExecutionWithResult<Set<Hardware>> getAllHardwares(Cloud cloud, String region,
			OperatingSystemArchitecture architecture);

	/**
	 * Removes and deletes the cloud infrastructure configuration.
	 * This method performs cleanup operations for the cloud provider configuration,
	 * which may include removing associated resources, credentials, or provider-specific settings.
	 * 
	 * @param cloud The cloud configuration to be removed and unregistered from the system
	 * @return A TaskExecutionWithResult with Void result indicating successful deletion when completed
	 */
	public TaskExecutionWithResult<Void> deleteInfrastructure(Cloud cloud);

}
