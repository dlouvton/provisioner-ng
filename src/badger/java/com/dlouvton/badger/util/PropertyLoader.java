package com.dlouvton.badger.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class PropertyLoader {

	private static Properties cachedTopLevelProperties;

	private PropertyLoader() {
	}

	public static Properties getPropertiesFromFile(File file) {
		return getPropertiesFromFile(file, null);
	}

	public static Properties getPropertiesFromFile(File file,
			Properties defaults) {
		// If "defaults" are given, overwrite them with new values.
		Properties props = defaults;
		if (props == null) {
			// If none are provided, create a new Properties object.
			props = new Properties();
		}

		try {
			props.load(new FileInputStream(file));
		} catch (Exception e) {
			throw new RuntimeException("Cannot load file "
					+ file.getAbsolutePath() + "!", e);
		}

		return props;
	}

	public static synchronized Properties getTopLevelProperties() {
		if (cachedTopLevelProperties == null) {
			Properties defaultProps = getPropertiesFromFile(new File(
					"default.properties"));
			cachedTopLevelProperties = getPropertiesFromFile(new File(
					"user.properties"), defaultProps);
		}

		return cachedTopLevelProperties;
	}
}
