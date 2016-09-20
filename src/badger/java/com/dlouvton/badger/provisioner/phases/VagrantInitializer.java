package com.dlouvton.badger.provisioner.phases;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.dlouvton.badger.api.server.BadgerServerException;
import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.Environment.ComponentsStatus;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.model.EnvironmentProperties;
import com.dlouvton.badger.provisioner.model.ModelException;
import com.dlouvton.badger.provisioner.model.YamlComponentStore;
import com.dlouvton.badger.provisioner.status.Phase;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.provisioner.status.StatusCode;

public class VagrantInitializer extends PhaseImpl {

	public VagrantInitializer(Environment environment) {
		super(environment);
	}

	public void execute() {
		if (model == null) {
			throw new ModelException(
					"No model file specified; did you call setModel?");
		}
		LOG.info("Loading model " + model.getModelParameters().get("name"));
		LOG.fine("Setting working directory to "
				+ model.getWorkingDirectory().getAbsolutePath()
				+ " (from model file)");

		try {
			checkNumberOfConcurrentEnvironments();
			validateVagrantInstallation();
			model.storeYamlInfo();
			checkComponentsState();
			verify();
			environment.setState(State.INITIALIZED);
		} catch (VagrantException e) {
			environment.markError(e, StatusCode.ModelInitializationError);
			throw new VagrantException("failed to initialize vagrant", e);
		} catch (IOException e) {
			environment.markError(e, StatusCode.ModelInitializationError);
			throw new VagrantException("failed to store Yaml information on "+YamlComponentStore.YAML_STORE_PATH, e);
		} finally {
			environment.uploadData();
		}
	}

	private void checkNumberOfConcurrentEnvironments() {
		if (model.getEnvironmentID() > VagrantProvisioner.MAX_NUM_ENVIRONMENTS
				|| model.getEnvironmentID() < 1) {
			throw new BadgerServerException("Badger supports up to "
					+ VagrantProvisioner.MAX_NUM_ENVIRONMENTS
					+ " concurrent environments");
		}
	}

	private void validateVagrantInstallation() {
		File vagrantfile = new File(wd, "Vagrantfile");
		if (!vagrantfile.exists()) {
			throw new VagrantException("Vagrantfile does not exist on " + wd);
		}
		cmd = newCommand();
		cmd.setService("");
		cmd.appendArg("-v");
		cmd.execute();
		String out = cmd.getStdout();
		if (out.contains("Vagrant"))
			LOG.info("vagrant installation check passed: " + out);
		else
			throw new VagrantException("Vagrant is not installed properly: "
					+ out);
	}

	// static nodes that are running are marked as <comp>_reused=true, to notify
	// the provisioners
	private void checkComponentsState() {
		// TODO: throw exception if the env does not support aws and
		// provider=aws
		Map<String, String> componentStatus = getVagrantComponentsStatus();
		environment.componentsStatus = new ComponentsStatus(componentStatus);
		for (Component comp : model.getComponents()) {
			String compName = comp.getLocalizedName();
			String componentState = componentStatus.get(compName + "_state");
			String componentProvider = componentStatus.get(compName + "_provider");
			boolean isRunning = "running".equals(componentState);

			LOG.fine(compName + ":" + componentState + "; static: "
					+ comp.isStatic());

			if (!comp.isStatic() && isRunning) {
				LOG.severe(compName
						+ " is running but was supposed to be destroyed.");
				if (properties.getProperty("destroy-leftover-dynamic-components").equalsIgnoreCase(
						"true")) {
					LOG.warning("Destroying "+compName+" (destroy-leftover-dynamic-components=true)");
					InstanceDestroyer destroyer = new InstanceDestroyer(
							environment);
					destroyer.cmd = newCommand();
					destroyer.destroyComponent(comp);
					environment.numDestroyedOnError++;
				} else {
					throw new VagrantException("Dynamic components are running and destroy-leftover-dynamic-components=false, Please destroy "
									+ compName);
				}
			}

			if (componentStatus.containsKey(compName + "_error")) {
				throw new VagrantException(compName
						+ " could not be started. "
						+ componentStatus.get(compName + "_error"));
			}
			//TODO need more generic method to detect unit testing.
			if (!comp.getProvider().equalsIgnoreCase(componentProvider) && !comp.getProvider().equalsIgnoreCase("managed")
					&& !model.getName().equals("utest")) {
				throw new VagrantException(comp.name
						+ " is not supported for provider "
						+ comp.getProvider());
			}

			EnvironmentProperties envProps = model.getEnvironmentProperties();
			if (!comp.getProvider().equalsIgnoreCase("managed") && comp.isStatic()
					&& isRunning) {
				LOG.info(compName + " is static and running, will reuse it");
				envProps.add(compName  + "_reused",	"true");
				environment.componentsStatus.add(compName  + "_reused", "true");
				environment.numReused ++;
			}

			envProps.add("environmentName" , model.getModelParameter("environmentName", "dev"));
			envProps.add("perforceUser" , properties.getProperty("perforce-user"));
			envProps.add("perforcePassword" , properties.getProperty("perforce-password"));
		}
	}

	public void verify() {
		if (model.getComponents() == null) {
			throw new VagrantException(
					"The model components were not initialized, did you call init() ?");
		}
	}

	public Phase getPhase() {
		return Phase.initialization;
	}

}
