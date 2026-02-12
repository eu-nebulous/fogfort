package eu.nebulouscloud.fogfort.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

public class LogWrapper implements AutoCloseable {

	Logger baseLogger;
	String outputLogFilePath;
	FileOutputStream outputLogFileStream;

	public LogWrapper(Logger baseLogger, String outputLogFileName) {
		this.baseLogger = baseLogger;
		try {
			this.outputLogFileStream = LogFileUtils.getInstance().getLogOutputStream(outputLogFileName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create output log file: " + outputLogFilePath, e);
		}
	}

	@Override
	public void close() {
		if (outputLogFileStream != null) {
			try {
				outputLogFileStream.close();
			} catch (IOException e) {

			}
		}
	}

	public void flush() {
		try {
			if (outputLogFileStream != null) {
				outputLogFileStream.flush();
			}
		} catch (IOException e) {

		}
	}

	/**
	 * Logs a message to both the regular logger and the taskExecution output log
	 * file (if available).
	 * 
	 * @param taskExecution The task execution to log to (may be null)
	 * @param message       The log message with optional placeholders
	 * @param args          Optional arguments for message formatting
	 */
	public void info(String message, Object... args) {
		// Log to regular logger
		baseLogger.info(formatMessage(message, args));

		// Log to taskExecution output log file if available
		try {
			// Format message with arguments
			String formattedMessage = formatMessage(message, args);
			outputLogFileStream.write(formattedMessage.getBytes());
		} catch (Exception e) {
			// Don't fail the operation if logging to file fails
			baseLogger.error(
					"Failed to write to taskExecution output log file: " + outputLogFilePath + " : " + e.getMessage());
			throw new RuntimeException("Failed to write to taskExecution output log file: " + outputLogFilePath, e);
		}
	}

	/**
	 * Logs a message to both the regular logger and the taskExecution output log
	 * file (if available).
	 * 
	 * @param taskExecution The task execution to log to (may be null)
	 * @param message       The log message with optional placeholders
	 * @param args          Optional arguments for message formatting
	 */
	public void debug(String message, Object... args) {
		// Log to regular logger
		baseLogger.debug(formatMessage(message, args));

		// Log to taskExecution output log file if available
		try {
			// Format message with arguments
			String formattedMessage = formatMessage(message, args);
			outputLogFileStream.write(formattedMessage.getBytes());
		} catch (Exception e) {
			// Don't fail the operation if logging to file fails
			baseLogger.error(
					"Failed to write to taskExecution output log file: " + outputLogFilePath + " : " + e.getMessage());
			throw new RuntimeException("Failed to write to taskExecution output log file: " + outputLogFilePath, e);
		}
	}

	public void warn(String message, Object... args) {
		baseLogger.warn(formatMessage(message, args));
		try {
			String formattedMessage = formatMessage(message, args);
			outputLogFileStream.write(formattedMessage.getBytes());
		} catch (Exception e) {
			baseLogger.error(
					"Failed to write to taskExecution output log file: " + outputLogFilePath + " : " + e.getMessage());
			throw new RuntimeException("Failed to write to taskExecution output log file: " + outputLogFilePath, e);
		}
	}

	public void error(String message, Object... args) {
		baseLogger.error(formatMessage(message, args));
		try {
			String formattedMessage = formatMessage(message, args);
			outputLogFileStream.write(formattedMessage.getBytes());
		} catch (Exception e) {
			baseLogger.error(
					"Failed to write to taskExecution output log file: " + outputLogFilePath + " : " + e.getMessage());
			throw new RuntimeException("Failed to write to taskExecution output log file: " + outputLogFilePath, e);
		}
	}

	private String formatMessage(String message, Object... args) {
		if (args == null || args.length == 0) {
			return message + System.lineSeparator();
		}

		String result = message;
		int consumedCount = 0;
		for (Object arg : args) {
			int placeholderIndex = result.indexOf("{}");
			if (placeholderIndex != -1) {
				String argString;
				if (arg == null) {
					argString = "null";
				} else if (arg instanceof Exception || arg instanceof RuntimeException) {
					// Handle exceptions with stacktrace
					Exception ex = (Exception) arg;
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					argString = sw.toString();
				} else {
					argString = arg.toString();
				}
				// Replace only the first {} placeholder at the specific position
				// This prevents issues when argString itself contains {}
				result = result.substring(0, placeholderIndex) + argString + result.substring(placeholderIndex + 2);
				consumedCount++;
			} else {
				break;
			}
		}

		// Append unconsumed arguments to the end
		if (consumedCount < args.length) {
			StringBuilder sb = new StringBuilder(result);
			for (int i = consumedCount; i < args.length; i++) {
				if (args[i] == null) {
					sb.append(" null");
				} else if (args[i] instanceof Exception || args[i] instanceof RuntimeException) {
					// Handle exceptions with stacktrace
					Exception ex = (Exception) args[i];
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					sb.append(" ").append(sw.toString());
				} else {
					sb.append(" ").append(args[i].toString());
				}
			}
			result = sb.toString();
		}

		return result + System.lineSeparator();
	}

}