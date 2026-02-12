/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package eu.nebulouscloud.fogfort.config;

import java.io.File;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import lombok.extern.slf4j.Slf4j;

@PropertySources({ @PropertySource(value = "classpath:application.properties"),
		@PropertySource(value = "file:${EXTERNAL_CONFIG_DIR}/${PROPERTIES_FILENAME}.properties", ignoreResourceNotFound = true) })
@Configuration
@Slf4j
public class RepositoryConfiguration {

	@Value("${spring.datasource.driverClassName:}")
	private String dataSourceDriverClassName;

	@Value("${spring.datasource.url:}")
	private String dataSourceUrl;

	@Value("${spring.datasource.username:}")
	private String dataSourceUsername;

	@Value("${fogfort.data.home:}")
	private String dataHome;

	@Value("${spring.datasource.password:}")
	private String dataSourcePassword;

	@Bean
	@Profile("default")
	public DataSource defaultDataSource() {
		log.info("Setting up prod data source");
		String jdbcUrl = dataSourceUrl;
		String driverClassName = dataSourceDriverClassName;

		if (jdbcUrl.isEmpty()) {
			jdbcUrl = "jdbc:hsqldb:file:" + getDatabaseDirectory()
					+ ";create=true;hsqldb.tx=mvcc;hsqldb.applog=1;hsqldb.sqllog=0;hsqldb.write_delay=false";
		}

		// If driver class name is not specified, default to HSQLDB driver
		if (driverClassName == null || driverClassName.isEmpty()) {
			driverClassName = "org.hsqldb.jdbc.JDBCDriver";
		}

		return DataSourceBuilder.create().username(dataSourceUsername).password(dataSourcePassword).url(jdbcUrl)
				.driverClassName(driverClassName).build();
	}

	@Bean
	@Profile("mem")
	public DataSource memDataSource() {
		log.warn("Setting up memory data source");
		return createMemDataSource();
	}

	@Bean
	@Profile("test")
	public DataSource testDataSource() {
		log.warn("Setting up test data source");
		return createMemDataSource();
	}

	private DataSource createMemDataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder.setType(EmbeddedDatabaseType.HSQL).addScript("create_text_type.sql").build();
		return db;
	}

	private String getDatabaseDirectory() {
		if (dataHome == null || dataHome.isEmpty())
			throw new RuntimeException("fogfort.data.home not set");
		return dataHome + File.separator + "fogfort" + File.separator + "db";

	}
}
