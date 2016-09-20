package com.dlouvton.badger.tracker;
/**
 * Tracker Interface: 
 * This interface is used to persist data from an underlying class into a persistence layer (such as MongoDB)
 * Implement this interface for every persistence layer. 
 */
public interface Tracker {
	/**
	 * get data object: the data object holds the data until it's being uploaded to the persistence layer.
	 * it must have key-value pairs
	 * @return object data object
	 */	
	public Object getData();
	
	/**
	 * upload the data object (getData()) into persistence layer
	 */
	public void uploadData();
	
	/**
	 * Insert a key-value pair into the data object (not uploading)
	 */
	public void put(String key, Object value);
	
	/**
	 * Update the data object on the persistence layer with new data (usually a map of key-value pairs) 
	 * @param newData new data to update
	 */
	public void appendData(Object newData);
	
	/**
	 * Dump the public fields in the underlying class into the data object
	 */
	public void dumpFields();
}
