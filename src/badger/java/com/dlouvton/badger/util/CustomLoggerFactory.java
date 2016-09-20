package com.dlouvton.badger.util;

import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomLoggerFactory {

	private static Level logLevel;

	private CustomLoggerFactory() {
	}

	// See:
	// http://stackoverflow.com/questions/470430/java-util-logging-logger-doesnt-respect-java-util-logging-level
	static {
		// get the top Logger:
		Logger topLogger = java.util.logging.Logger.getLogger("");

		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : topLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}
		}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			topLogger.addHandler(consoleHandler);
		}

		// set the console handler to the right level
		consoleHandler.setLevel(getLogLevel());
	}

	public static Logger getLogger(String name) {
		Logger log = Logger.getLogger(name);
		log.setLevel(getLogLevel());

		return log;
	}

	@SuppressWarnings("rawtypes")
	public static Logger getLogger(Class theClass) {
		return getLogger(theClass.getName());
	}

	private static synchronized Level getLogLevel() {
		if (logLevel == null) {
			Properties props = PropertyLoader.getTopLevelProperties();

			if (props.containsKey("log-level")) {
				// If we have the key, parse and use it
				try {
					logLevel = Level.parse(props.getProperty("log-level"));
				} catch (IllegalArgumentException iae) {
					// We can directly create a logger, we're special :)
					Logger logger = Logger.getLogger(CustomLoggerFactory.class
							.getName());
					logger.warning("Error parsing log-level setting; defaulting to INFO.");
					logLevel = Level.INFO;
				}
			} else {
				// If we don't, default to info
				logLevel = Level.INFO;
			}
		}

		return logLevel;
	}
}
