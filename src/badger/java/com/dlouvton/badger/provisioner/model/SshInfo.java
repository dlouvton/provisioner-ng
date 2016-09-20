package com.dlouvton.badger.provisioner.model;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.dlouvton.badger.provisioner.VagrantCommand;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.util.CustomLoggerFactory;

public class SshInfo {

	private Component component;
	private Model model;
	public Logger LOG = CustomLoggerFactory.getLogger(SshInfo.class);

	public SshInfo(Component component, Model model) {
		this.component = component;
		this.model = model;
	}

	// On aws, properties from "vagrant ssh-config" are incorrect.
	// You see properties like this:
	// kafka_Host=kafka
	// kafka_HostName=192.168.6.148
	// kafka_name=kafka
	// "Host" and "name" refer only to the box name in the Vagrantfile.
	// Clearly "HostName" isn't right either; instead, it should be
	// "ip-192-168-6-148".
	// For these cases, correct the HostName and add an IPAddress.
	public void updateHostnameAndIP() throws IOException {
                if (component.properties.containsKey("HostName")) {
			String rawHostName = component.properties.get("HostName");

			// Only do the corrections if we're on aws, or if it's a managed server that is connected to a aws box
			if (component.getProvider().equals("aws") || component.getAttribute("server", "").contains("_hostname")) {
				
				LOG.fine("Correcting hostname and IP of " + rawHostName);

				// Hostname is predictable
				// 1.2.3.4 --> ip-1-2-3-4
                String correctedHostName = rawHostName.contains(".") ? "ip-" + rawHostName.replace('.', '-') : rawHostName;

				if (component.componentExposesProperty(model, "HostName")) {
					component.properties.put("HostName", correctedHostName);
				}
				if (component.componentExposesProperty(model, "IPAddress")) {
					component.properties.put("IPAddress", rawHostName);
				}
					
				//update the Yaml store with the hostname, so managed servers retrieve the hostname and connect to aws boxes
				YamlComponentStore.append(component.getLocalizedName()+"_hostname: " + rawHostName + System.getProperty("line.separator"));
				
			}
		}
	}

	// "vagrant ssh-config" returns ssh information such as host name, port,
	// private key and more. all of it will be exposed to the
	// environment.properties, unless you specify exactly what to expose
	public void updateSshConfig(VagrantCommand cmd) throws InterruptedException {
		cmd.setService("ssh-config");
		cmd.setTargetMachine(component.name);
		cmd.execute();
		if (cmd.getExitValue() != 0)
			throw new VagrantException("Failed to get SSH info for component "+component.name+". It's probably not ready for SSH");
		List<String> tokens = cmd.getStdoutLines();

		for (String token : tokens) {
			String[] configLine = token.trim().split(" ");

			if (configLine.length != 2) {
				continue;
			}

			String key = configLine[0].trim();
			String value = configLine[1].trim();
			if (component.componentExposesProperty(model, key)) {
				component.properties.put(key, value);
			}
		}
	}
}
