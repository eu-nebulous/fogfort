package eu.nebulouscloud.fogfort.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import eu.nebulouscloud.fogfort.dto.IpAddress;
import eu.nebulouscloud.fogfort.dto.IpAddressType;
import eu.nebulouscloud.fogfort.dto.IpVersion;
import eu.nebulouscloud.fogfort.model.SSHConnectionParameters;
import eu.nebulouscloud.fogfort.model.jobs.Task.TaskExecution;

/**
 * Example test for NodeScriptRunner.runScript() method.
 * 
 * To run this test: 1. Update the SSH connection parameters below with your
 * actual SSH server details 2. Ensure you have a valid SSH private key 3. Run
 * the test using your IDE or: ./gradlew test --tests NodeScriptRunnerTest
 * 
 * Note: This test requires a real SSH server to connect to. For unit testing,
 * consider mocking the SSH connection or using a test SSH server.
 */
@SpringBootTest
@ActiveProfiles("test")
class NodeScriptRunnerTest {

	private static String TEST_VM_IP = "44.221.48.202";

	@Autowired
	private NodeScriptRunner nodeScriptRunner;

	/**
	 * Reads the SSH private key from test resources.
	 * 
	 * @return The private key content as a string
	 * @throws IOException if the resource file cannot be read
	 */
	private String readPrivateKeyFromResource() throws IOException {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("default-2026.pem");
		if (inputStream == null) {
			throw new IOException("default-2026.pem not found in test resources");
		}
		try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
	}

	@Test
	void testRunScript() throws Exception {
		// Create SSH connection parameters
		SSHConnectionParameters sshParams = new SSHConnectionParameters();

		// Set IP address
		IpAddress ipAddress = new IpAddress();
		ipAddress.setIpAddressType(IpAddressType.PUBLIC_IP);
		ipAddress.setIpVersion(IpVersion.V4);
		ipAddress.setValue(TEST_VM_IP); // Replace with your SSH server IP
		sshParams.setIpAddress(ipAddress);

		// Set SSH credentials
		sshParams.setUsername("ubuntu"); // Replace with your SSH username
		sshParams.setPort("22"); // Default SSH port, change if needed

		// Set private key from test resource
		sshParams.setPrivateKey(readPrivateKeyFromResource());

		// Create script execution result object
		TaskExecution result = new TaskExecution();

		// Define the script to execute
		String script = "echo 'Hello from remote server!' && hostname && date";

		// Set timeout (in milliseconds) - 30 seconds
		Long timeout = 30000L;

		// Execute the script asynchronously
		CompletableFuture<Void> future = nodeScriptRunner.runScript(script, sshParams, result, timeout);

		// Wait for the result (with a maximum wait time)
		future.get(60, TimeUnit.SECONDS);
		future.get(60, TimeUnit.SECONDS);

		// Check if execution was successful
		if (result.isSuccess()) {
			System.out.println("Script execution successful!");
			System.out.println("Output: " + result.getOutputLogFileName());
			System.out.println("Success: " + result.isSuccess());
			System.out.println("Start time: " + result.getStart());
			System.out.println("End time: " + result.getEnd());
		}
	}

	/**
	 * Example test with a simple command that should work on most Unix-like systems
	 */
	@Test
	void testRunScriptSimpleCommand() throws Exception {
		// Create SSH connection parameters
		SSHConnectionParameters sshParams = new SSHConnectionParameters();

		IpAddress ipAddress = new IpAddress();
		ipAddress.setIpAddressType(IpAddressType.PUBLIC_IP);
		ipAddress.setIpVersion(IpVersion.V4);
		ipAddress.setValue(TEST_VM_IP); // Replace with your SSH server IP
		sshParams.setIpAddress(ipAddress);

		sshParams.setUsername("ubuntu"); // Replace with your SSH username
		sshParams.setPort("22");
		sshParams.setPrivateKey(readPrivateKeyFromResource());

		TaskExecution result = new TaskExecution();

		// Simple command that should work on most systems
		String script = "uname -a";

		Long timeout = 10000L; // 10 seconds

		CompletableFuture<Void> future = nodeScriptRunner.runScript(script, sshParams, result, timeout);

		future.get(30, TimeUnit.SECONDS);

		if (result.isSuccess()) {
			System.out.println("Command executed successfully!");
			System.out.println("Output:\n" + result.getOutputLogFileName());
		} else {
			System.err.println("Error: " + result.getOutputLogFileName());
		}
	}
}
