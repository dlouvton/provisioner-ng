package com.dlouvton.badger.provisioner.status;
/***
 * Phases of Provisioning:
 * Every environment goes through the following phases, in order:
 * initialization, vm_creation, installation, configuration, teardown (optional)
 */
public enum Phase {
	initialization, vm_creation, installation, configuration, teardown
};

