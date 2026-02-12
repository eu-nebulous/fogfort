package eu.nebulouscloud.fogfort.cloud;

public class CloudProviderException extends Exception {
	public CloudProviderException(String message) {
		super(message);
	}

	public CloudProviderException(String message, Throwable cause) {
		super(message, cause);
	}

	public CloudProviderException(Throwable cause) {
		super(cause);
	}

}
