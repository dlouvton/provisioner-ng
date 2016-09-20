package com.dlouvton.badger.provisioner.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import com.dlouvton.badger.util.PropertyLoader;

public class YamlComponentStore {

	private Properties props = PropertyLoader.getTopLevelProperties();
	public static final String YAML_STORE_PATH = System.getProperty("user.home")
			+ "/.vagrantuser";
	private StringBuffer yamlComponentsBuffer = new StringBuffer(
			String.format("components: %n"));
	private StringBuffer yamlManagedBuffer = new StringBuffer(
			String.format("managed: %n"));
	private StringBuffer yamlAwsInfoBuffer = new StringBuffer(
			String.format("aws: %n"));

	
	private void addComponentBlock(Component comp, StringBuffer buffer) {
		Map<String, String> props = comp.properties;
		buffer.append(String.format(" %s:%n", comp.name));
		for (Entry<String, String> entry : props.entrySet()) {
			buffer.append(line(entry));
		}
	}

	public void addComponent(Component comp) {
		addComponentBlock(comp, yamlComponentsBuffer);
	}

	public void addManagedComponent(Component comp) {
		addComponentBlock(comp, yamlManagedBuffer);
	}

	private static String line(Entry<String, String> entry) {
		return String.format("  %s: %s%n", entry.getKey(), entry.getValue());
	}

	public void writeToFile() throws IOException {
		new File(YAML_STORE_PATH).delete();  // ignore the returned value
		BufferedWriter out = new BufferedWriter(new FileWriter(YAML_STORE_PATH));
		out.write(String.format("model: \"%s\"%n", props.getProperty("model-path").replaceFirst("models/", "")));
		out.write(yamlAwsInfoBuffer.toString());
		out.write(yamlComponentsBuffer.toString());
		out.write(yamlManagedBuffer.toString());
		out.flush();
		out.close();
	}

	public void addAwsInfo() {
		yamlAwsInfoBuffer.append(String.format(" private_key: %s%n",
				props.getProperty("private-key")));
		yamlAwsInfoBuffer.append(String.format(" private_key_path: %s%n",
				props.getProperty("private-key-path")));
		yamlAwsInfoBuffer.append(String.format(" endpoint: %s%n",
				props.getProperty("endpoint")));
	}

	public static void append(String line) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(YAML_STORE_PATH, true));
		out.write(line);
		out.close();
	}

}
