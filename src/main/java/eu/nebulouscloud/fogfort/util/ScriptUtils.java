package eu.nebulouscloud.fogfort.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScriptUtils {

	public static String getScriptFromUrl(String url) {
		try {
			URI uri = URI.create(url);
			try (InputStream inputStream = uri.toURL().openStream()) {
				return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			log.error("Error getting script from URL: {}", url, e);
			return null;
		}
	}




	private static String getEnvVarsScript(Map<String, String> envVars, String user) {
		String script = "";
		for (Map.Entry<String, String> entry : envVars.entrySet()) {
			script += "echo 'export " + entry.getKey() + "=\"" + entry.getValue() + "\"' >> /home/ubuntu/.profile\n";
		}
		return script;
	}

	private static String getEnvVarsSource(String user) {
		return "source /home/" + user + "/.profile";
	}

	public static String getMasterInstallScripts(String user, Map<String, String> envVars) {
		/*String[] urls = {
				"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/MASTER_INSTALL_SCRIPT.sh",
				"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/MASTER_PRE_INSTALL_SCRIPT.sh",
				"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/MASTER_START_SCRIPT.sh" };
		String script = "";
		for (String url : urls) {
			script += getScriptFromUrl(url) + "\n";
		}
		return getEnvVarsSource(user) + "\n" + script;*/
		return  "echo 'Hello from master!' && hostname && date";
	}

	public static String getWorkerInstallScripts(String user, Map<String, String> envVars) {
		/*String[] urls = {
		"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/WORKER_INSTALL_SCRIPT.sh",
		"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/WORKER_PRE_INSTALL_SCRIPT.sh",
		"https://raw.githubusercontent.com/eu-nebulous/sal-scripts/refs/heads/main/installation-scripts-onm/WORKER_START_SCRIPT.sh" };
		String script = "";
		for (String url : urls) {
		script += getScriptFromUrl(url) + "\n";
		}
		return getEnvVarsSource(user) + "\n" + script;*/
		return  "echo 'Hello from worker!' && hostname && date && echo 'Kubeadm join command: $variables_kubeCommand'";
	}

	public static String getKubeadmJoinCommandScript() {
		//return ScriptUtils.getScriptFromResource("create_kubeadm_token.sh");
		return  "echo '!!NEB_SCRIPT_RESULT_KUBERNETES_JOIN_TOKEN:this-is-a-dummy-token!!'";
	}


	public static String getScriptFromResource(String scriptName) {
		try (InputStream inputStream = ScriptUtils.class.getClassLoader()
				.getResourceAsStream(scriptName)) {
			if (inputStream == null) {
				log.error("Could not find resource: {}", scriptName);
				return null;
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Error reading {}", scriptName, e);
			return null;
		}
	}

}
