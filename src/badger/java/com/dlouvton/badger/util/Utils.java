package com.dlouvton.badger.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
/*
 * A collection of static utilities used by Badger
 * These utilities include converting file content to string; getting folder content and more.
 */
public class Utils {

	private static Logger LOG = CustomLoggerFactory
			.getLogger(Utils.class);
	/**
	 * Converting a file into String
	 * 
	 * @param path to file
	 * @return String containing data
	 */
	public static String fileToString(String path) throws IOException {
		StringBuffer buffer = new StringBuffer();
		String thisLine;
		BufferedReader br = new BufferedReader(new FileReader(path));
		while ((thisLine = br.readLine()) != null) {
			buffer.append(thisLine);
		}
		br.close();
		return buffer.toString();
	}
		
	//returns a nicely formatted duration string, i.e. 22 min, 3 sec
	public static String formatMilliseconds(long millis) {
		return String.format(
				"%d min, %d sec",
				TimeUnit.MILLISECONDS.toMinutes(millis),
				TimeUnit.MILLISECONDS.toSeconds(millis)
						- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
								.toMinutes(millis)));
	}

	//a list of files and folders in a folder
	public static List<String> getFolderFileNames(String path) {
		File f = new File(path);
		return new ArrayList<String>(Arrays.asList(f.list()));
	}
	
	//clean up a map in a thread-safe manner 
	public static void removeAllEntriesFromMap(Map<?,?> map) {
		Iterator<?> it = map.entrySet().iterator();
		while (it.hasNext())
		{
		  it.next();
		  it.remove();
		 }
	}

	//Valid component names must contain only alphanumeric characters and undersocore. No dashes, white spaces etc
	public static boolean isValidComponentName(String name) {
		return name.matches("^[a-zA-Z0-9_]*$");
	}
	
	/**
	 * inject an exception that is not checked. Useful to inject exception in unit tests
	 * @param exception exception to throw
	 * @throws T
	 */
	@SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwException(Throwable exception) throws T
    {
        throw (T) exception;
    }
	
	/**
	 * Log a nicely readable severe message 
	 * @param e Exception to log
 	 * @param message message to log
	 */
	public static void logSevere(Exception e, String message)
	{
		LOG.severe("-----------------------------------------------------------------------------");
		LOG.severe(message+": "+e.getMessage());
		e.printStackTrace();
		LOG.severe("-----------------------------------------------------------------------------");

	}

}
