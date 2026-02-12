package eu.nebulouscloud.fogfort.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import eu.nebulouscloud.fogfort.util.LogFileUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Configuration
@PropertySources({ @PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "file:${EXTERNAL_CONFIG_DIR}/${PROPERTIES_FILENAME}.properties", ignoreResourceNotFound = true) })
@Getter
@Setter
public class FogFortApplicationConfiguration {
	@Value("${fogfort.output.log.directory:}")
	private String outputLogDirectory;

	public String getOutputFilePath(String fileName) {
		return outputLogDirectory + File.separator + fileName;
	}

	@Value("${fogfort.security.disabled:false}")
	private Boolean securityDisabled;

	@PostConstruct
	public void init() {
		LogFileUtils.init(this);
	}

}
