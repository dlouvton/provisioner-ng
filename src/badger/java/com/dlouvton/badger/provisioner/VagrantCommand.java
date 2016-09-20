package com.dlouvton.badger.provisioner;

import java.io.File;
import java.util.List;

import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.util.ShellCommand;

public class VagrantCommand extends ShellCommand {

	private static final String executable = "vagrant";

	public boolean serviceIsSet = false;
	private int provisionCalls = 0;
	public boolean reused=false;

	public VagrantCommand(File workDirectory) {
		super(workDirectory);
	}

	public VagrantCommand(File workDirectory, String outputPrefix) {
		super(workDirectory, outputPrefix);
	}
	
	public ShellCommand setService(String service) {
		setExecutable(executable);
		serviceIsSet = true;
		return appendArg(service);
	}

	public ShellCommand setProvisioner(String provisioner) {
		provisionCalls++;
		return appendOption("--provision-with ").appendOption(provisioner);
	}

	public ShellCommand setProvider(String provider) {
		return appendOption("--provider=" + provider);
	}

	public ShellCommand debug() {
		return appendOption("--debug");
	}

	public ShellCommand machineReadable() {
		return appendOption("--machine-readable");
	}

	public ShellCommand noProvision() {
		provisionCalls++;
		return appendOption("--no-provision");
	}

	public ShellCommand setTargetMachines(List<Component> target) {
		if (target.isEmpty()) {
			throw new VagrantException("target component list is empty");
		}

		return appendArg(Component.componentListToString(target));
	}

	public ShellCommand setTargetMachine(String target) {
		if (target.trim().isEmpty()) {
			throw new VagrantException("target component is empty");
		}

		return appendArg(" " + target);
	}

	@Override
	public void execute() {
		if (!serviceIsSet || provisionCalls > 1) {
			throw new VagrantException(
					"The Vagrant command must contain a service (i.e. up, destroy) "
							+ "and no more than 1 provision call");
		}

		super.execute();
	}
}
