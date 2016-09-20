package com.dlouvton.badger.provisioner;

import java.util.Map;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.status.Status;

public interface Provisioner {
	// model initialization and verification
	public Environment init(Model model) throws SetupException,ResourceBusyException;

	// Aggregation of the up, install and configure
	public void setup(Environment environment, boolean destroyOnError) throws SetupException,ResourceBusyException;
		
	// bring down components except static ones
	public void destroy(Environment environment) throws SetupException;
	
	// get all environments 
	public Map<Integer, Environment>  getEnvironments();
	
	// get system status
	public Status getStatus();
}
