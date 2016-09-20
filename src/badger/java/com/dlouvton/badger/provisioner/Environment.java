package com.dlouvton.badger.provisioner;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.mongodb.BasicDBObject;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.status.Phase;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.provisioner.status.StatusCode;
import com.dlouvton.badger.tracker.MongoTracker;

/** 
 * The Environment class holds information about a running environment
 * Such as model name, status and components status.
 * It's identified by an environment ID 
 * 
 */
@XmlRootElement
public class Environment extends MongoTracker {

	private Map<String, String> provisionPhases = new HashMap<String, String>();
	private Model model;
	private Map<String, String> envProperties;
	public ComponentsStatus componentsStatus;
	public String environmentStatus;
	public String setupResult;
	public String modelName;
	public String errorMessage;
	public String user;
	public long startTime;
	public int environmentId;
	public int statusCode;
	public int numFailedTests;
	public int numPassedTests;
	public String testSuiteName;
	public String testEndTime;
	public int numDestroyedOnError;
	public int numDestroyed;
	public int numComponents;
	public int numReused;
	public int numSetupAttempts;
	public String modelFileName;
	public String setupDuration;
	public String hostname;
	public String destroyError;
	private long destroyedAt;

	public Environment(Model model) {
		super();
		this.model = model;
		modelName = model.getName();
		modelFileName = model.getFileName();
		numComponents = model.getComponents().size();
		startTime = Long.valueOf(getIdentifier());
		user = System.getenv("TAG_USERNAME");
		environmentId = model.getEnvironmentID();
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// it will be assigned null and not recorded
		}
		for (Phase phase : Phase.values()) {
			update(phase, State.NOT_STARTED);
		}

	}

	public Environment() {
		super();
	}

	public Model getModel() {
		return model;
	}

	/**
	 * checks whether the environment is in a given state
	 * 
	 * @param state Given state (e.g. State.BUSY, State.READY)
	 * 
	 * @return boolean true if it is in the given state
	 */
	public boolean isStateEquals(State state) {
		return environmentStatus.equals(state.name());
	}

	public void update(Phase phase, State state) {
		provisionPhases.put(phase.name(), state.name());
	}

	/**
	 * Mark the environment as 'reused' (basically setup is skipped), signaling
	 * that it's being reused
	 */
	public void markReused() {
		update(Phase.vm_creation, State.SKIPPED);
		update(Phase.installation, State.SKIPPED);
		update(Phase.configuration, State.SKIPPED);
		setState(State.SKIPPED);
	}

	/**
	 * set the state to 'error', signaling that it's not usable
	 * 
	 * @param e Exception that occured in this environment
	 * @param statusCode update statusCode with this value
	 */
	public void markError(Throwable e, StatusCode statusCode) {
		setState(State.ERROR);
		errorMessage = e.getMessage();
		if (e.getCause() != null) {
			errorMessage = errorMessage + "; Cause: "
					+ e.getCause().getMessage();
		}
		this.statusCode = statusCode.code;
	}

	/**
	 * set the state to 'interrupted', signaling that it was stopped by user
	 * 
	 */
	public void markInterrupted() {
		environmentStatus = State.INTERRUPTED.name();
		errorMessage = "setup was interrupted";
		this.statusCode = StatusCode.Interrupted.code;
	}
	
	/**
	 * mark this environment as ready to use
	 * @param None
	 */
	public void markReady() {
		setState(State.READY);
		this.statusCode = StatusCode.Success.code;
	}

	/**
	 * mark this environment as destroyed
	 * @param None
	 */
	public void markDestroyed() {
		setupResult = environmentStatus;
		environmentStatus = State.DESTROYED.name();
		destroyedAt  = System.currentTimeMillis();
		BasicDBObject updateQuery = new BasicDBObject(); 
		BasicDBObject fieldSets = new BasicDBObject(); 
		fieldSets.put("environmentStatus", environmentStatus); 
		fieldSets.put("destroyedAt", destroyedAt); 
		fieldSets.put("numDestroyed", numDestroyed); 
		fieldSets.put("destroyError", destroyError); 
		fieldSets.put("setupResult", setupResult); 
		updateQuery.put( "$set", fieldSets); 
		appendData(updateQuery);
	}
	
	/**
	 * update this environment with stats about tests
	 * @param None
	 */
	public void updateWithTestStats() {
		BasicDBObject updateQuery = new BasicDBObject(); 
		BasicDBObject fieldSets = new BasicDBObject(); 
		fieldSets.put("numFailedTests", numFailedTests); 
		fieldSets.put("numPassedTests", numPassedTests); 
		fieldSets.put("testSuiteName", testSuiteName); 
		fieldSets.put("testEndTime", testEndTime); 
		updateQuery.put( "$set", fieldSets); 
		appendData(updateQuery);
	}
	
	/**
	 * sets the environment status to a certain state
	 * @param state to set
	 */
	public void setState(State state) {
		environmentStatus = state.name();
		setupResult = state.name();
	}

	/**
	 * get environment properties
	 * @return Map map of environment properties
	 */
	public Map<String, String> getEnvProperties() {
		return envProperties;
	}

	/**
	 * sets the environment properties from a map
	 * @param envProperties map of environment properties
	 */
	
	public void setEnvProperties(Map<String, String> envProperties) {
		this.envProperties = envProperties;
	}

	/**
	 *  get Provision Phases
	 * @return Map map of environment properties
	 */
	public Map<String, String> getProvisionPhases() {
		return provisionPhases;
	}

	/**
	 * sets the provision phases from a map
	 * @param provisionPhases map of provision phases
	 */

	public void setProvisionPhases(Map<String, String> provisionPhases) {
		this.provisionPhases = provisionPhases;
	}

	/**
	 * serializable map of components and their status (for example, could be populated by 'vagrant status')
	 * has an underlying map of entries
	 */
	@XmlRootElement
	public static class ComponentsStatus {
		
		public Map<String, String> entries;

		public ComponentsStatus(Map<String, String> entries) {
			this.entries = entries;
		}
		
		public void add(String key, String value) {
			this.entries.put(key, value);
		}
		
		public String toString() {
			return entries.toString();
		}
		public ComponentsStatus() {

		}
	}

}
