package com.dlouvton.badger.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

// See http://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html?page=2
class StreamGobbler extends Thread {

	private final InputStream inputStream;
	private final String outputPrefix;
	private final StringBuffer stringBuffer;
	private final boolean outputToConsole;
	private static final String[] EXCLUDED_LINES = 
		{"Unrecognized arguments: endpoint"};

	private static final String LS = System.getProperty("line.separator");
	private static final Logger LOG = CustomLoggerFactory
			.getLogger(StreamGobbler.class);

	StreamGobbler(InputStream is, String outputPrefix) {
		this(is, outputPrefix, null, true);
	}

	StreamGobbler(InputStream is, String outputPrefix, StringBuffer sb,
			boolean outputToConsole) {
		this.inputStream = is;
		this.outputPrefix = outputPrefix;
		this.stringBuffer = sb;
		this.outputToConsole = outputToConsole;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while ((line = br.readLine()) != null) {
				if (this.outputToConsole && !shouldExcludeLine(line)) {
					LOG.fine(" " + outputPrefix + " > " + line);
				}

				// Save to the StringBuilder as well so you can get it later
				if (this.stringBuffer != null) {
					stringBuffer.append(line);
					stringBuffer.append(LS);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private boolean shouldExcludeLine(String logLine) {
		for(String excluded : EXCLUDED_LINES) {
			if(logLine.contains(excluded))
				return true;
		}
		
		return false;
	}
}
