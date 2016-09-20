package com.dlouvton.badger.provisioner.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.dlouvton.badger.provisioner.model.Graph.Edge;

public class Component {

	public String name;
	public final HashSet<Edge> inEdges;
	public final HashSet<Edge> outEdges;
	public final Map<String, String> properties;
	private String defaultProvider;

	public Component(String name, Map<String, String> properties,
			String defaultProvider) {
		this.name = name;
		this.inEdges = new HashSet<Edge>();
		this.outEdges = new HashSet<Edge>();
		this.defaultProvider = defaultProvider;
		this.properties = properties;
		if (!properties.containsKey("role")) 
			properties.put("role", name);
	}

	public String getAttribute(String key, String defaultValue) {
		if (properties.containsKey(key)) {
			return properties.get(key);
		}

		return defaultValue;
	}

	public String getProvider() {
		return getAttribute("provider", defaultProvider);
	}

	public String getRole() {
		return getAttribute("role", name);
	}
	
	/* 
	 * return a component name that could be used on the local environment, stripping "__<envID>
	 * @return component name that does not contain env ID
	 */
	public String getLocalizedName() {
		return name.replaceAll("__[0-9]+$", "");
	}
	
	public Component addEdge(Component node) {
		Edge e = new Edge(this, node);
		outEdges.add(e);
		node.inEdges.add(e);
		return this;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isStatic() {
		return "true".equals(getAttribute("static", "false"));
	}

	public String[] getExposedProperties() {
		if (this.properties.containsKey("expose")) {
			return this.properties.get("expose").split(",");
		}

		return new String[0];
	}

	public static String componentListToString(List<Component> components) {
		String str = "";

		for (Component c : components) {
			str += c.toString() + " ";
		}

		return str.trim();
	}

	public boolean componentExposesProperty(Model model,
			String property) {
		String[] exposedProps = model.getExposedParameters();
		// Empty lists mean expose all
		if (exposedProps.length == 0 && getExposedProperties().length == 0) {
			return true;
		}

		// Otherwise, the property must be in either of the lists
		for (String s : exposedProps) {
			if (s.equals(property)) {
				return true;
			}
		}
		for (String s : getExposedProperties()) {
			if (s.equals(property)) {
				return true;
			}
		}

		return false;
	}
}
