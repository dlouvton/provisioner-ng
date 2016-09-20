package com.dlouvton.badger.provisioner.status;
/***
 * States are used to describe the state of a certain a Phase or an Environment, or to describe the "global" Status (the Provisioner status)
 * 
 */
public enum State {
		NOT_STARTED, STARTED, INITIALIZED, COMPLETED, READY, SKIPPED, ERROR, DESTROYED, BUSY, INTERRUPTED
	};

