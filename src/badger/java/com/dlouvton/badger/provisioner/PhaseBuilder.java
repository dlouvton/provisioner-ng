package com.dlouvton.badger.provisioner;

import com.dlouvton.badger.provisioner.phases.InstanceCreator;
import com.dlouvton.badger.provisioner.phases.InstanceDestroyer;
import com.dlouvton.badger.provisioner.phases.PhaseImpl;
import com.dlouvton.badger.provisioner.phases.PuppetInstaller;
import com.dlouvton.badger.provisioner.phases.ShellConfigurator;
import com.dlouvton.badger.provisioner.phases.VagrantInitializer;
import com.dlouvton.badger.provisioner.status.Phase;
import com.dlouvton.badger.provisioner.status.StatusCode;
/*
 * This is a Builder class that takes a phase name and runs the appropriate ProvisionPhase implementation.
 * One the implementation is selected, it runs execute() on it, and marks it as started, completed or error.
 */
public class PhaseBuilder {
	
	public static PhaseImpl runPhase(Phase phase, Environment environment) throws VagrantException {
		PhaseImpl phaseImp;
		if (phase.equals(Phase.initialization)) {
			phaseImp= new VagrantInitializer(environment);
		} else if (phase.equals(Phase.vm_creation)) {
			phaseImp= new InstanceCreator(environment);
		} else if (phase.equals(Phase.installation)) {
			phaseImp = new PuppetInstaller(environment);
		} else if (phase.equals(Phase.configuration)) {
			phaseImp= new ShellConfigurator(environment);
		} else if (phase.equals(Phase.teardown)) {
			phaseImp= new InstanceDestroyer(environment);
		} else
			throw new IllegalArgumentException("No such provision phase "
					+ phase);
		phaseImp.cmd = (VagrantCommand) VagrantProvisioner.getInstance().getCommandLineService();
		try {
			phaseImp.markStarted();
			phaseImp.execute();
			phaseImp.markCompleted();
		} catch (VagrantException e) {
			phaseImp.markError(e, StatusCode.SystemError);
		}
		return phaseImp;
	}
}