package com.dlouvton.badger.provisioner.phases;

import java.util.List;
import java.util.Map;

import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.status.Phase;
import com.dlouvton.badger.util.ShellCommand;

/*
 * This ProvisioPhase destroys an environment.
 * It has multiple options:
 * By default it will destroy all non-static components in the environment
 * You can configure it to 'forceDestroyGlobalComponents', 'forceDestroyLocalComponents' or 'dontDestroyLocalReusedComponents'
 */
public class InstanceDestroyer extends PhaseImpl {

	List<Component> dynamicComponents;
	// exclude reused static components
	private boolean dontDestroyLocalReusedComponents = false; 

	public InstanceDestroyer(Environment environment) {
		super(environment);
		this.dynamicComponents = model.getDynamicComponents();
	}

	/*
	 * execute normal workflow: destroy only dynamic components of the current model
	 * Not destroying statics
	 */
	public void execute() {
		if (dynamicComponents.size() > 0) {
			try {
				LOG.info("Tearing down environment: destroying all dynamic components");
				cmd = newCommand();
				cmd.setService("destroy");
				cmd.appendOption("-f");
				cmd.setTargetMachines(dynamicComponents);
				cmd.execute();
				verify();
			} catch (VagrantException e) {
				environment.destroyError+=e.getMessage()+"; ";
				throw new VagrantException("Error while destroying the model", e);
			}
		} else {
			LOG.info(" -- Skipping teardown; there are no dynamic components.");
		}
		environment.numDestroyed = dynamicComponents.size();
		environment.markDestroyed();
		
	}

	/*
	 * static components that are reused will not be destroyed,
	 * if you call this method
	 */
	public void dontDestroyLocalReusedComponents() {
		dontDestroyLocalReusedComponents = true;
	}

	/*
	 * Destroy a component
	 * @param component the component to destroy
	 */
	public void destroyComponent(Component component) {
		try {
			if (dontDestroyLocalReusedComponents
					&& isStaticAndReused(component)) {
				LOG.info("Will not destroy component " + component.name
						+ " (it is static but reused)");
			} else {
				LOG.info(" -- Destroying component " + component.name);
				cmd = newCommand();
				cmd.setService("destroy");
				cmd.appendOption("-f");
				cmd.setTargetMachine(component.name);
				cmd.execute();
				verifyComponentIsDestroyed(component);
			}
		} catch (VagrantException e) {
			environment.destroyError+= component.name+": "+ e.getMessage()+"; ";
			throw new VagrantException("Error while destroying component "
					+ component.name, e);
		}
	}

	/*
	 * Verify that components were destroyed correctly By making sure that
	 * dynamic components are at state 'not_created'
	 */
	public void verify() {
		verifyVMsAreInState(dynamicComponents, "not_created",
				"verification: model is destroyed");
	}

	/*
	 * Verify that a component is destroyed correctly By making sure that it's
	 * not at state 'running'
	 * 
	 * @param component Component name
	 */
	public void verifyComponentIsDestroyed(Component component) {
		Map<String, String> componentStatus = getVagrantComponentsStatus();
		if ("running".equals(componentStatus.get(component.name + "_state"))) {
			throw new VagrantException(	"the VM '" + component.name
				+ "' is in state 'running', which is not the desired state, not_created.");
		}
	}

	/*
	 * All existing model components be destroyed, including static, if you call
	 * this method.
	 */
	public void destroyAllModelComponents() {
		LOG.info("Tearing down environment: destroying all model components, including statics");
		Map<String, String> componentStatus = getVagrantComponentsStatus();
		int destroyCount = 0;
		for (Component component : model.getComponents()) {
			if ("running".equals(componentStatus.get(component.name + "_state"))) {
				destroyComponent(component);
				destroyCount++;
			}
		}
		environment.numDestroyed = destroyCount;
		environment.markDestroyed();
	}

	/*
	 * All global components (that started from this host), including static,
	 * will be destroyed if you call this method.
	 */
	public static void destroyAll() {
		ShellCommand cmd = new ShellCommand(model.getWorkingDirectory());
		cmd.clear();
		cmd.setExecutable("sh");
		cmd.appendOption("-c");
		cmd.appendArg("../setup/terminateAllInstances.sh");
		cmd.execute();
		for (Environment env : VagrantProvisioner.getInstance().getEnvironments().values()) {
			env.numDestroyed = env.numComponents;
			env.markDestroyed();
		}
	}

	public Phase getPhase() {
		return Phase.teardown;
	}

}
