package eu.nebulouscloud.fogfort.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import eu.nebulouscloud.fogfort.cloud.CloudProviderException;
import eu.nebulouscloud.fogfort.dto.ErrorResponse;

@ControllerAdvice
public class CustomExceptionHandler {
	@ExceptionHandler(value = IllegalArgumentException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(value = CloudProviderException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleCloudProviderException(CloudProviderException ex) {
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(value = MissingServletRequestParameterException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
		String message = String.format("Required request parameter '%s' for method parameter type %s is not present",
				ex.getParameterName(), ex.getParameterType());
		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
	}

	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
		String message = String.format("Can't process '%s'", ex.getMessage());

		return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
	}

}