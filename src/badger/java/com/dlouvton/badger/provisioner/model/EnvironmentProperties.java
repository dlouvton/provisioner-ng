package com.dlouvton.badger.provisioner.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import static java.nio.file.StandardOpenOption.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.dlouvton.badger.util.CustomLoggerFactory;
import com.dlouvton.badger.util.PropertyLoader;


public class EnvironmentProperties {

	public final Logger LOG = CustomLoggerFactory.getLogger(getClass());
	
	private final Properties prop = new Properties();
	private final File fileName;

	public EnvironmentProperties(File fileName) {
		this.fileName = fileName;
	}

	public File getFile() {
		return fileName;
	}
	
	/*
	 * add key=value to the Environment Properties
	 */
	public void add(String key, String value) {
		prop.setProperty(key, value);
	}

	/* get a property from the Environment Properties
	 * This is used for querying from within test cases. 
	 * @param property key
	 * @return property value 
	 */
	public String getProperty(String key) {
		return prop.getProperty(key);
	}

	/* Also used for querying from within test cases. See above note.
	 * @return a set of all property names
	 */
	public Set<String> getPropertyNames() {
		return prop.stringPropertyNames();
	}

	public void addComponent(Component component) throws IOException {
		for (String key : component.properties.keySet()) {
			add(component.getLocalizedName() + "_" + key,	component.properties.get(key));
		}
	}
	
	
	public void writeToFile() throws IOException {
		// For readability of the file, and ease of debugging, sort the keys.
		// This puts all the keys for each VM together, and also sorts the keys
		// within a VM.
		//
		// http://stackoverflow.com/questions/17011108/how-can-i-write-java-properties-in-a-defined-order
		@SuppressWarnings("serial")
		Properties tempProps = new Properties() {
			@Override
			public synchronized Enumeration<Object> keys() {
				return Collections.enumeration(new TreeSet<Object>(super
						.keySet()));
			}
		};
		tempProps.putAll(prop);
		
		Properties properties = PropertyLoader.getTopLevelProperties();
		boolean performSetup = true;   // Default unless over-ridden
		if (properties.containsKey("perform-setup")
			&& properties.getProperty("perform-setup").equalsIgnoreCase("false")) {
				performSetup = false;	
				LOG.fine("perform-setup=false, so appending to "+this.fileName);			
		}
			
		OutputStream output = Files.newOutputStream(this.fileName.toPath(), CREATE, 
		                                 performSetup?TRUNCATE_EXISTING:APPEND);
		try {
			tempProps.store(output, "Badger created file based on model ");   // TODO include model name (not currently in properties)
		} finally {
			output.close();
		}
		
		// first, create the stage/vagrant directory if does not exist
		File stageFolder = new File("stage/vagrant");
		if (!stageFolder.exists()) {
			LOG.warning("Folder 'stage/vagrant' not found, creating it. ");
			stageFolder.mkdirs(); 
		}

		// Make a copy in the stage/vagrant directory for future preform-setup=false runs.
		File stageCopy = new File( stageFolder , this.fileName.getName());
		// Create the file, if does not exist.
		stageCopy.createNewFile(); 
		output = new FileOutputStream(stageCopy);
		try {
			tempProps.store(output, "Badger created file based on model "+ getProperty("model-path"));
		} finally {
			output.close();
		} 
	}
}