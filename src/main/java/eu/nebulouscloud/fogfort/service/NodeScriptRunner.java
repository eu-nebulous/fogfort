package eu.nebulouscloud.fogfort.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import eu.nebulouscloud.fogfort.model.SSHConnectionParameters;
import eu.nebulouscloud.fogfort.model.jobs.Task.TaskExecution;
import eu.nebulouscloud.fogfort.util.LogFileUtils;
import eu.nebulouscloud.fogfort.util.LogWrapper;
import lombok.extern.slf4j.Slf4j;

@Service("NodeScriptRunner")
@Slf4j
public class NodeScriptRunner {

	@Async
	public CompletableFuture<Void> runScript(String script, SSHConnectionParameters sshConnectionParameters,
			TaskExecution result, Long timeout) {
		return CompletableFuture.runAsync(() -> {
			Session session = null;
			Channel channel = null;
			try (LogWrapper logWrapper = new LogWrapper(log, result.getOutputLogFileName())) {
				try {
					logWrapper.info("Starting script execution at " + new Date());
					// Validate SSH connection parameters
					if (sshConnectionParameters == null) {
						throw new IllegalArgumentException("SSH connection parameters cannot be null");
					}

					if (sshConnectionParameters.getIpAddress() == null
							|| sshConnectionParameters.getIpAddress().getValue() == null) {
						throw new IllegalArgumentException("IP address is required for SSH connection");
					}

					if (sshConnectionParameters.getUsername() == null
							|| sshConnectionParameters.getUsername().isEmpty()) {
						throw new IllegalArgumentException("Username is required for SSH connection");
					}

					if (sshConnectionParameters.getPrivateKey() == null
							|| sshConnectionParameters.getPrivateKey().isEmpty()) {
						throw new IllegalArgumentException("Private key is required for SSH connection");
					}

					String host = sshConnectionParameters.getIpAddress().getValue();
					int port = sshConnectionParameters.getPort() != null && !sshConnectionParameters.getPort().isEmpty()
							? Integer.parseInt(sshConnectionParameters.getPort())
							: 22;
					String username = sshConnectionParameters.getUsername();
					String privateKey = sshConnectionParameters.getPrivateKey();

					logWrapper.info("Connecting to {}@{}:{} via SSH", username, host, port);
					// Create JSch instance
					JSch jsch = new JSch();

					// Add private key
					jsch.addIdentity("temp-key", privateKey.getBytes(), null, null);

					// Create session
					session = jsch.getSession(username, host, port);

					// Disable strict host key checking (for development - consider using
					// known_hosts in production)
					java.util.Properties config = new java.util.Properties();
					config.put("StrictHostKeyChecking", "no");
					session.setConfig(config);

					// Set connection timeout
					session.setTimeout(30000); // 30 seconds

					result.setStart(new Date());
					// Capture output

					int exitStatus = -1;
					try (OutputStream outputStream = LogFileUtils.getInstance()
							.getLogOutputStream(result.getOutputLogFileName())) {
						// Connect
						int retryCount = 0;
						while (!session.isConnected() && retryCount < 3) {
							try {
								session.connect();
								break;
							} catch (Exception e) {
								logWrapper.error("Error connecting to {}@{}:{} via SSH, retrying in 5 seconds... ({}/3)", username,
										host, port, retryCount + 1, e);
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e1) {
									Thread.currentThread().interrupt();
									throw new RuntimeException("Interrupted while waiting for SSH connection", e1);
								}
							}
							retryCount++;
						}
						logWrapper.info("SSH connection established to {}@{}:{}", username, host, port);
						// Record start time
						// Execute script
						channel = session.openChannel("exec");
						channel.setOutputStream(outputStream);
						((ChannelExec) channel).setErrStream(outputStream);

						
						logWrapper.debug("===============================================================");
						logWrapper.debug("===============================================================");
						logWrapper.debug("Executing script: {}", script);
						((ChannelExec) channel).setCommand(script);
						logWrapper.debug("===============================================================");
						logWrapper.debug("===============================================================");
						// Connect channel and wait for completion
						logWrapper.debug("Connecting to channel");
						channel.connect();
						logWrapper.debug("Channel connected");

						// Wait for channel to be closed (script execution completed)
						long waitStartTime = System.currentTimeMillis();
						long timeoutMillis = timeout != null ? timeout : Long.MAX_VALUE; // Use provided timeout or wait
																							// indefinitely

						while (!channel.isClosed()) {
							// Check if timeout has been exceeded
							long elapsedTime = System.currentTimeMillis() - waitStartTime;
							outputStream.flush();
							if (elapsedTime > timeoutMillis) {
								logWrapper.error("Script execution timed out after " + timeoutMillis + "ms");
								throw new RuntimeException("Script execution timed out after " + timeoutMillis + "ms");
							}
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								logWrapper.error("Script execution interrupted", e);
								throw new RuntimeException("Script execution interrupted", e);
							}
						}

						// Get exit status
						exitStatus = channel.getExitStatus();
					} catch (Exception ex) {
						logWrapper.error("Error executing script via SSH", ex);
					}

					// Record end time
					result.setEnd(new Date());
					logWrapper.info("Script execution ended at " + new Date());
					result.setSuccess(exitStatus == 0);
					if (exitStatus != 0) {
						logWrapper.warn("Script execution failed with exit status {} on {}@{}:{}", exitStatus, username,
								host, port);
					} else {
						logWrapper.info("Script executed successfully on {}@{}:{}", username, host, port);
					}
					return;

				} catch (JSchException e) {
					logWrapper.error("SSH connection error", e);
					result.setEnd(new Date());
					result.setSuccess(false);
					logWrapper.error("SSH connection error: " + e.getMessage());
					return;
				} catch (Exception e) {
					logWrapper.error("Error executing script via SSH", e);
					logWrapper.error("Error executing script: " + e.getMessage());
					result.setEnd(new Date());
					result.setSuccess(false);
					return;
				} finally {
					// Clean up resources
					if (channel != null && channel.isConnected()) {
						channel.disconnect();
					}
					if (session != null && session.isConnected()) {
						session.disconnect();
					}
				}
			}
		});
	}

	public Map<String,String> extractScriptResults(TaskExecution result) {
		Map<String,String> results = new HashMap<>();
		
		if (result == null || result.getOutputLogFileName() == null) {
			return results;
		}
		
		// Pattern to match !!NEB_SCRIPT_RESULT_<KEY>:<VALUE>!!
		Pattern pattern = Pattern.compile("!!NEB_SCRIPT_RESULT_([^:]+):([^!]+)!!");
		
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(LogFileUtils.getInstance().getLogInputStream(result.getOutputLogFileName())))) {
			
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String key = matcher.group(1);
					String value = matcher.group(2);
					results.put(key, value);
				}
			}
		} catch (IOException e) {
			log.error("Error reading output log file: {}", result.getOutputLogFileName(), e);
		}
		
		return results;
	}

}
