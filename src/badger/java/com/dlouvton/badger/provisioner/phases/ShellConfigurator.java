package com.dlouvton.badger.provisioner.phases;

import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.status.Phase;

public class ShellConfigurator extends PhaseImpl {

	public ShellConfigurator(Environment environment) {
		super(environment);
	}

	int exitStatus;
	String lastComponentName;

	public void configure(Component component) {
		LOG.info("Configuring component " + component.name);
		cmd = newCommand(component.name);
		cmd.setService("provision");
		cmd.setTargetMachine(component.name);
		cmd.setProvisioner("shell");
		cmd.execute();
		exitStatus = cmd.getExitValue();
	}

	public void execute() {
		for (Component component : model.getComponents()) {
			if (isStaticAndReused(component) && Boolean.valueOf(properties.getProperty("reconfigure-static-components")) == false) {
				LOG.info("Skipping configuration for component "+component.name);
				continue;
			}
			configure(component);
			lastComponentName = component.name;
			verify();
		}

	}

	public void verify() {
		if (exitStatus != SUCCESS) {
			throw new VagrantException(
					"component "+lastComponentName+" is not configured properly; configuration script returned "
							+ exitStatus);
		}
	}
	
	public Phase getPhase() {
		return Phase.configuration;
	}
}
