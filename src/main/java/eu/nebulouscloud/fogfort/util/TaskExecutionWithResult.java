package eu.nebulouscloud.fogfort.util;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;

import eu.nebulouscloud.fogfort.model.jobs.Task;
import lombok.Getter;
import lombok.Setter;

/**
 * A Result type that can hold either a success value or an exception.
 * 
 * @param <T> The type of the success value
 */
@Getter
@Setter
public class TaskExecutionWithResult<T> extends Task.TaskExecution {
	private T result;
	private Exception exception;

	public TaskExecutionWithResult(Date start) {
		super();
		this.start = start;
		this.success = false;

	}

	public TaskExecutionWithResult(T result, Date start, Date end, boolean success, String outputLogFileName) {
		this.start = start;
		this.outputLogFileName = outputLogFileName;
		this.end = end;
		this.success = success;
		this.result = result;
	}

	public TaskExecutionWithResult<T> withException(Exception exception) {
		this.end = new Date();
		this.success = false;
		this.result = null;
		this.exception = exception;
		return this;
	}

	public TaskExecutionWithResult<T> mergeLogs(TaskExecutionWithResult<?> other) throws IOException {
		LogFileUtils.getInstance().mergeLogs(other.getOutputLogFileName(), this.outputLogFileName);
		return this;
	}

	public boolean hasException() {
		return this.exception != null;
	}

	public TaskExecutionWithResult<T> withResult(T result) {
		this.end = new Date();
		this.success = true;
		this.result = result;
		this.exception = null;
		return this;
	}

	public LogWrapper getLogWrapper(Logger log) {
		return new LogWrapper(log, this.outputLogFileName);
	}

}
