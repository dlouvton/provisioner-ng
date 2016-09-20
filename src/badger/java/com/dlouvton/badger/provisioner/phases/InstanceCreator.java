package com.dlouvton.badger.provisioner.phases;

import java.io.IOException;
import java.util.List;

import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.model.SshInfo;
import com.dlouvton.badger.provisioner.status.Phase;

public class InstanceCreator extends PhaseImpl {

	public static final int NUMBER_OF_TRIES = 4;
	public static final int WAIT_TIME_MS_SEC = 60;
	private int tryNum = -1;
	InstanceDestroyer destroyerOnError;

	public InstanceCreator(Environment environment) {
		super(environment);
		// destroy all components on error, including newly created statics, but
		// not reused statics
		destroyerOnError = new InstanceDestroyer(environment);
		destroyerOnError.dontDestroyLocalReusedComponents();
	}

	@Override
	public void execute() {
		LOG.info(" -- Bringing up environment...");
		destroyerOnError.cmd = cmd;
		RetryStrategy retry = new RetryStrategy(NUMBER_OF_TRIES, cmd.reused ? 0
				: WAIT_TIME_MS_SEC);
		while (retry.shouldRetry()) {
			try {
				tryNum = retry.tryNum();
				environment.numSetupAttempts = tryNum;
				attempt();
				break;
			} catch (VagrantException ve) {
				try {
					ve.printStackTrace();
					LOG.warning("an error occured, retrying based on retry policy");
					LOG.fine("Components status: "
							+ getVagrantComponentsStatus().toString());
					LOG.info("Waiting " + retry.getTimeToWait()
							+ " sec before the next retry");
					retry.errorOccured();
				} catch (RetryException re) {
					throw new VagrantException("Error while running the "
							+ getPhase().name() + " phase, after attempting "
							+ NUMBER_OF_TRIES + " times", re);
				}
			}
		}
	}

	private void attempt() {
		// unless create-instances-in-parallel=false, doing parallel
		// installation on first attempt
		boolean createInstancesInParallel = Boolean
				.valueOf(model.getModelParameter(
						"create-instances-in-parallel",
						properties.getProperty("create-instances-in-parallel")));

		if (tryNum == 1 && createInstancesInParallel) {
			// on first try we bring all default components in parallel
			LOG.fine("Create instances in parallel, try 1");
			createInstances(model.getDefaultProviderComponents());
		} else {
			// on subsequent tries, we will bring them up one by one
			LOG.fine("Create instances serially, try " + tryNum);
			for (Component comp : model.getDefaultProviderComponents()) {
				createInstance(comp);
			}
		}
		updateHostsInfo(model.getDefaultProviderComponents());

		for (Component otherComp : model.getNonDefaultProviderComponents()) {
			createInstance(otherComp);
		}

		updateHostsInfo(model.getNonDefaultProviderComponents());
		updateEnvProperties();
		verify();
	}

	private void updateEnvProperties() {
		try {
			for (Component component : model.getComponents()) {
				envProps.addComponent(component);
			}
			envProps.writeToFile();
			LOG.fine("Updated environment.properties");
		} catch (IOException ioe) {
			throw new VagrantException(
					"Error while updating environment properties", ioe);
		}
	}

	private void createInstances(List<Component> components) {
		cmd = newCommand();
		cmd.setService("up");
		cmd.setTargetMachines(components);
		cmd.setProvider(model.getProvider());
		cmd.noProvision();
		cmd.comment("attempt " + tryNum);
		cmd.execute();
		if (cmd.getExitValue() != SUCCESS) {
			throw new VagrantException(
					"components were not created properly when brought up in parallel");
		}
	}

	private void createInstance(Component comp) {
		cmd = newCommand(comp.name);
		cmd.setService("up");
		cmd.setTargetMachine(comp.name);
		cmd.setProvider(comp.getProvider());
		cmd.noProvision();
		cmd.comment(comp.name + " up attempt " + tryNum);
		cmd.execute();
		if (cmd.getExitValue() != SUCCESS) {
			destroyerOnError.destroyComponent(comp);
			environment.numDestroyedOnError++;
			throw new VagrantException("component " + comp.name
					+ " was not created properly");
		}
	}

	public void updateHostsInfo(List<Component> componentList) {
		for (Component component : componentList) {
			try {
				SshInfo info = new SshInfo(component, model);
				info.updateSshConfig(newCommand());
				info.updateHostnameAndIP();
			} catch (IOException e) {
				LOG.severe("error while updating ssh information");
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void verify() {
		List<Component> components = model.getComponents();
		verifyVMsAreInState(components, "running", "verification " + tryNum);
	}

	public Phase getPhase() {
		return Phase.vm_creation;
	}

}
