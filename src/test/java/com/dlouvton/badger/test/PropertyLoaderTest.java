package com.dlouvton.badger.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.Test;

import com.dlouvton.badger.util.PropertyLoader;

// This class uses the default.properties and user.properties files that are checked in.
// If the contents of those files change, test cases here may need to be altered.
public class PropertyLoaderTest {

	@Test
	public void topLevelProperties() {
		// Top level properties is just a convenience call
		Properties topLevelProps = PropertyLoader.getTopLevelProperties();
		Properties manualProps = PropertyLoader.getPropertiesFromFile(new File(
				"user.properties"), PropertyLoader
				.getPropertiesFromFile(new File("default.properties")));

		// Make sure all entries are the same for both
		Enumeration<?> propNames = topLevelProps.propertyNames();
		while (propNames.hasMoreElements()) {
			String key = propNames.nextElement().toString();
			if (!key.equals("provision-time")) { //skip this one, as it's dynamic
				 assertEquals(topLevelProps.getProperty(key),
					manualProps.getProperty(key));
			}
		}
	}

	@Test
	public void readOneFile() {
		Properties prop = PropertyLoader.getPropertiesFromFile(new File(
				"src/resources/defaults.properties"));

		assertTrue(prop.containsKey("property-in-both-files"));
		assertEquals("default-value",
				prop.getProperty("property-in-both-files"));

		assertFalse(prop.containsKey("key-does-not-exist"));
	}

	@Test
	public void readTwoFiles() {
		Properties defaultProp = PropertyLoader.getPropertiesFromFile(new File(
				"src/resources/defaults.properties"));
		Properties overrideProp = PropertyLoader.getPropertiesFromFile(
				new File("src/resources/overrides.properties"), defaultProp);

		// Property overridden appropriately
		assertTrue(overrideProp.containsKey("property-in-both-files"));
		assertEquals("override-value",
				overrideProp.getProperty("property-in-both-files"));

		// Property only in default is present
		assertTrue(overrideProp.containsKey("property-in-default-file-only"));
		assertEquals("a-value",
				overrideProp.getProperty("property-in-default-file-only"));

		// Property only in override is present
		assertTrue(overrideProp.containsKey("property-in-override-file-only"));
		assertEquals("some-value",
				overrideProp.getProperty("property-in-override-file-only"));

		// Missing properties still missing
		assertFalse(overrideProp.containsKey("key-does-not-exist"));
	}

	@Test(expected = RuntimeException.class)
	public void fileMissing() throws Exception {
		PropertyLoader.getPropertiesFromFile(new File(
				"does-not-exist.properties"));
	}
}
