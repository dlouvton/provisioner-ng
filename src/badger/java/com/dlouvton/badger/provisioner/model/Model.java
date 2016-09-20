package com.dlouvton.badger.provisioner.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dlouvton.badger.util.Utils;

public class Model {

	private JsonNode rootNode;
	private JsonNode envNode;
	private Map<String, Component> componentsMap;
	private Map<String, String> modelParams;
	private File modelFile;
	private List<Component> components;
	private static final String DEFAULT_PROVIDER = "aws";
	private EnvironmentProperties environmentProperties;
	private List<Component> componentsWithDefaultProvider;
	private List<Component> componentsWithNonDefaultProvider;
	private int environmentID;

	public Model(File model, int environmentID) {
		try {
			modelFile = model;
			this.setEnvironmentID(environmentID);
			rootNode = new ObjectMapper().readTree(modelFile);
			envNode = rootNode.get("env");
			initialize();
		} catch (IOException e) {
			throw new ModelException("Error creating environment model from "
					+ model.getAbsolutePath(), e);
		}
	}

	public String getName() {
		return getModelParameter("name", getFileName());
	}

	public String getFileName() {
		return modelFile.getName();
	}

	public List<Component> getComponents() {
		return components;
	}

	public List<Component> getDefaultProviderComponents() {
		return componentsWithDefaultProvider;
	}

	public List<Component> getNonDefaultProviderComponents() {
		return componentsWithNonDefaultProvider;
	}

	public String getProvider() {
		if (modelParams.containsKey("provider")) {
			return modelParams.get("provider");
		}

		return DEFAULT_PROVIDER;
	}

	// populate the components (nodes) with the json information under "env"
	// populate the env properties with the top-level json information
	private void initialize() {
		componentsMap = new HashMap<String, Component>();
		modelParams = getModelParameters(rootNode);
		for (JsonNode componentField : envNode) {
			String name = getNodeName(componentField);

			// Component names are put onto a command line. Having unbounded
			// names is a Bad Thing. For simplicity, ensure they're alphanumeric
			// only. This also removes the possibility of spaces.
			if (!Utils.isValidComponentName(name)) {
				throw new ModelException(
						"Component names must contain only alphanumeric characters and undersocore. No dashes, white spaces etc.  Offending name: "
								+ name);
			}
			addComponent(name, getModelParameters(componentField));
		}
		setEnvironmentProperties();
		topsortComponents();
		renameComponents();
		componentsWithDefaultProvider = new ArrayList<Component>();
		componentsWithNonDefaultProvider = new ArrayList<Component>();

		for (Component comp : components) {
			if (getProvider().equals(comp.getProvider())) {
				componentsWithDefaultProvider.add(comp);
			} else {
				componentsWithNonDefaultProvider.add(comp);
			}
		}
	}

	private void addComponent(String componentName,
			Map<String, String> componentParams) {
		// adds a model-level static entry, with the value "false" by default
		if (!componentParams.containsKey("static")) {
			componentParams.put("static", getModelParameter("static", "false"));
		}
		componentsMap.put(componentName, new Component(componentName,
				componentParams, getProvider()));
	}

	public void storeYamlInfo() throws IOException {
		try {
			YamlComponentStore yaml = new YamlComponentStore();
			yaml.addAwsInfo();
			for (Component comp : components) {
				if ("managed".equals(comp.getProvider())) {
					yaml.addManagedComponent(comp);
				} else
					yaml.addComponent(comp);
			}
			yaml.writeToFile();
		} catch (IOException ioe) {
			throw new IOException("failed to store Yaml information in "
					+ YamlComponentStore.YAML_STORE_PATH);
		}
	}

	private Map<String, String> getModelParameters(JsonNode node) {
		Map<String, String> params = new HashMap<String, String>();
		Iterator<Entry<String, JsonNode>> nodeIterator = node.fields();
		while (nodeIterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodeIterator
					.next();
			if (entry.getValue().isTextual()) {
				params.put(entry.getKey(),
						substitute(entry.getValue().asText()));
			}
		}

		return params;
	}

	private String substitute(String text) {
		return text.replace("<<RANDOM>>",
				Integer.toString((int) Math.floor(Math.random() * 99999)));
	}

	public Component getComponent(String name) {
		if (componentsMap.containsKey(name)) {
			return componentsMap.get(name);
		}

		return null;
	}

	private Map<String, Component> addDependencies() {
		for (JsonNode arrayElem : envNode) {
			Component comp = componentsMap.get(getNodeName(arrayElem));
			if (arrayElem.has("before")) {
				for (JsonNode dep : arrayElem.get("before")) {
					String dependentStr = dep.asText();
					// if it's a valid component name, add it as a dependency
					if (Utils.isValidComponentName(dependentStr)) {
						comp.addEdge(getComponent(dependentStr));
						// othewise, treat the dependent string as a regex
					} else {
						for (String componentName : componentsMap.keySet()) {
							if (componentName.matches(dependentStr)) {
								comp.addEdge(getComponent(componentName));
							}
						}
				}
			}
		}
		}
		return componentsMap;
	}

	private String getNodeName(JsonNode node) {
		if (node.isMissingNode()) {
			throw new ModelException(
					"A component is missing a name!  All components must have names.");
		}

		return node.get("name").asText();
	}

	public void topsortComponents() {
		if (componentsMap.isEmpty()) {
			throw new ModelException("Error: The model has no components");
		}

		componentsMap = addDependencies();
		components = Graph.topsort(componentsMap.values());
	}

	// append "__<envID>" to component names, so we could have multiple
	// environment on Vagrant
	private void renameComponents() {
		for (Component component : components) {
			component.name = component.name + "__" + environmentID;
		}
	}

	private void setEnvironmentProperties() {
		File environmentPropertiesFile = new File(getWorkingDirectory(),
				".environment" + environmentID + ".properties");

		// Always create an environment.properties
		environmentProperties = new EnvironmentProperties(
				environmentPropertiesFile);
	}

	public EnvironmentProperties getEnvironmentProperties() {
		return environmentProperties;
	}

	public List<Component> getDynamicComponents() {
		ArrayList<Component> list = new ArrayList<Component>();

		for (Component c : getComponents()) {
			if (!c.isStatic()) {
				list.add(c);
			}
		}

		return list;
	}

	public File getWorkingDirectory() {
		return new File(getModelParameter("working_dir", "."));
	}

	/* return an array of exposed ssh-config fields based on the model field "expose"
	* e.g. ["HostName","IPAddress"]
	* if the "expose" field does not exist, will expose HostName and IPAddress by default.
	*/
	public String[] getExposedParameters() {
		return getModelParameter("expose", "HostName,IPAddress").split(",");
	}

	public Map<String, String> getModelParameters() {
		return modelParams;
	}

	public String getModelParameter(String key, String defaultValue) {
		if (modelParams.containsKey(key))
			return modelParams.get(key);
		else
			return defaultValue;
	}

	public int getEnvironmentID() {
		return environmentID;
	}

	public void setEnvironmentID(int environmentID) {
		this.environmentID = environmentID;
	}

	public String getComponentsStr() {
		return Arrays.toString(components.toArray());
	}
}
