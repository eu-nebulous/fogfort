package eu.nebulouscloud.fogfort.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.util.StreamUtils;

import eu.nebulouscloud.fogfort.config.FogFortApplicationConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogFileUtils {

	private static LogFileUtils instance;

	public static LogFileUtils getInstance() {
		if (instance == null) {
			instance = new LogFileUtils();
		}
		return instance;
	}

	public static void init(FogFortApplicationConfiguration fogFortApplicationConfiguration) {
		if (instance == null) {
			instance = new LogFileUtils();
		}
		instance.fogFortApplicationConfiguration = fogFortApplicationConfiguration;
	}

	private FogFortApplicationConfiguration fogFortApplicationConfiguration;

	public String getOutputFilePath(String fileName) {
		return fogFortApplicationConfiguration.getOutputFilePath(fileName);
	}

	public void appendLineToOutputLog(String line, String outputLogFileName) throws IOException {
		appendOutputLog(line + "\n", outputLogFileName);
	}

	public void appendOutputLog(String output, String outputLogFileName) throws IOException {
		createOutputLogFile(outputLogFileName);
		File logFile = new File(fogFortApplicationConfiguration.getOutputFilePath(outputLogFileName));
		try (FileOutputStream outputLog = new FileOutputStream(logFile, true)) {
			outputLog.write(output.getBytes());
		}
	}

	private void createOutputLogFile(String outputLogFileName) throws IOException {
		if (outputLogFileName == null || outputLogFileName.isEmpty())
			throw new FileNotFoundException("Output log file path is null or empty");
		File logFile = new File(fogFortApplicationConfiguration.getOutputFilePath(outputLogFileName));
		if (!logFile.exists()) {
			try {
				File parentDir = logFile.getParentFile();
				if (parentDir != null && !parentDir.exists()) {
					parentDir.mkdirs();
				}
				logFile.createNewFile();
			} catch (IOException e) {
				throw new FileNotFoundException("Failed to create output log file on path: " + logFile.getAbsolutePath()
						+ " : " + e.getMessage());
			}
		}
	}

	public FileInputStream getLogInputStream(String outputLogFileName) throws IOException {
		createOutputLogFile(outputLogFileName);
		return new FileInputStream(fogFortApplicationConfiguration.getOutputFilePath(outputLogFileName));
	}

	public FileOutputStream getLogOutputStream(String outputLogFileName) throws IOException {
		createOutputLogFile(outputLogFileName);
		return new FileOutputStream(fogFortApplicationConfiguration.getOutputFilePath(outputLogFileName), true);
	}

	public void mergeLogs(String sourceLogFileName, String destinationLogFileName) throws IOException {
		try (InputStream logInputStream = getLogInputStream(sourceLogFileName)) {
			String logContent = StreamUtils.copyToString(logInputStream, StandardCharsets.UTF_8);
			appendLineToOutputLog(System.lineSeparator() + logContent, destinationLogFileName);
		}
		File logFile = new File(fogFortApplicationConfiguration.getOutputFilePath(sourceLogFileName));
		if (logFile.exists()) {
			logFile.delete();
		}
	}

	@PostConstruct
	public void testWriteOnInit() {
		try {
			String testMessage = "Test write on application initialization - " + java.time.LocalDateTime.now() + "\n";
			appendOutputLog(testMessage, "init-test.log");
			log.info("Test write executed successfully on initialization");
		} catch (IOException e) {
			log.error("Failed to execute test write on initialization", e);
		}
	}

}