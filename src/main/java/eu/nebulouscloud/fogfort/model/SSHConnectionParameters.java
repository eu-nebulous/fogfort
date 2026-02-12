package eu.nebulouscloud.fogfort.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.nebulouscloud.fogfort.dto.IpAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class SSHConnectionParameters implements Serializable { // Can't extend SSHCredentials because it's an embeddable
																// class

	@Column(name = "USERNAME")
	@JsonProperty("username")
	private String username = null;

	@Column(name = "KEY_PAIR_NAME")
	@JsonProperty("keyPairName")
	@JsonIgnore
	private String keyPairName = null;

	@Lob
	@Column(name = "PUBLIC_KEY")
	@JsonProperty("publicKey")
	@JsonIgnore
	private String publicKey = null;

	@Lob
	@Column(name = "PRIVATE_KEY")
	@JsonProperty("privateKey")
	@JsonIgnore
	private String privateKey = null;

	@Embedded
	private IpAddress ipAddress;

	@Column(name = "PORT")
	@JsonProperty("port")
	private String port = null;
}
