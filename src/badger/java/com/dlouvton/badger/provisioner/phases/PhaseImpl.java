package com.dlouvton.badger.provisioner.phases;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.model.EnvironmentProperties;
import com.dlouvton.badger.provisioner.status.PhaseListener;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.provisioner.status.StatusCode;
import com.dlouvton.badger.util.CustomLoggerFactory;
import com.dlouvton.badger.util.PropertyLoader;
import com.dlouvton.badger.util.ShellCommand;

public abstract class PhaseImpl implements ProvisionPhase, PhaseListener {

	public final Logger LOG = CustomLoggerFactory.getLogger(getClass());
	public static Model model;
	public Environment environment;
	public File wd;
	public static final int SUCCESS = 0;
	public EnvironmentProperties envProps;
	public Properties properties;
	public VagrantCommand cmd; // just for unit tests, we will reuse cmd

	public PhaseImpl(Environment environment) {
		try {
			model = environment.getModel();
			wd = model.getWorkingDirectory();
			properties = PropertyLoader.getTopLevelProperties();
			envProps = model.getEnvironmentProperties();
			this.environment = environment;
		} catch (NullPointerException npe) {
			throw new VagrantException("cannot execute the phase "+ getPhase().toString() +"; environment is not initialized properly");
		}
		
	}

	// returns status of all components, including state, provider, isStatic
	// we're doing it serially for every component, since vagrant doesn't print
	// component name
	public Map<String, String> getVagrantComponentsStatus() {
		Map<String, String> map = new HashMap<String, String>();
		for (Component component : model.getComponents()) {
			if ("dummy".equals(component.getProvider())) {
				map.put("dummy", "true");
				return map;
			}
			String componentName = component.getLocalizedName();
			map.put(componentName + "_HostName", component.getAttribute("HostName",null));

			cmd = newCommand();
			cmd.setService("status");
			cmd.setTargetMachine(component.name);
			cmd.machineReadable();
			cmd.comment("componentStatus");
			cmd.execute();

			// the output from the command looks like that:
			// 1411400572,,provider-name,aws
			// 1411400572,,state,not_created
			java.util.List<String> lines = cmd.getStdoutLines();
			for (String line : lines) {
				String[] fields = line.split(",");
				String fieldName = fields[2];
				String fieldValue = fields[3];

				if ("error-exit".equals(fieldName)) {
					map.put(componentName + "_error", fieldValue);
				}

				if ("state".equals(fieldName)) {
					map.put(componentName + "_state", fieldValue);
					map.put(componentName + "_isStatic",
							String.valueOf(component.isStatic()));
				}
				if ("provider-name".equals(fieldName)) {
					map.put(componentName + "_provider", fieldValue);
				}

			}
		}
		return map;
	}

	public VagrantCommand newCommand() {
		if (cmd.reused) {
			return cmd;
		}

		return new VagrantCommand(wd);
	}

	public VagrantCommand newCommand(String compName) {
		if (cmd.reused) {
			return cmd;
		}
		return new VagrantCommand(wd, compName);
	}

	// returns the count of vms at a certain state
	public int countVMsInState(String state, String comment) {
		cmd = newCommand();
		cmd.setService("status");
		cmd.machineReadable();
		cmd.setTargetMachines(model.getComponents());
		cmd.pipeline("grep 'state," + state + "'").pipeline("wc -l")
				.comment(comment);
		cmd.execute();
		String out = cmd.getStdout().trim();

		return Integer.parseInt(out.isEmpty() ? "0" : out);
	}

	public void verifyVMsAreInState(List<Component> componentList,
			String state, String comment) {
		String componentsString = Component
				.componentListToString(componentList);
		int count = countVMsInState(state, comment);
		LOG.fine("About to verify that " + componentList.size()
				+ " VMs are at state " + state + " (Component list: ["
				+ componentsString + "])");

		if (count != componentList.size()) {
			throw new VagrantException(count + " VMs are in state " + state
					+ " when " + componentList.size() + " were expected");
		}
	}

	// !!! Careful, this won't work right if sshcmd has double-quotes in it.
	public static VagrantCommand ssh(String componentName, String sshcmd,
			ShellCommand cmdService) throws VagrantException {
		VagrantCommand cmd = (VagrantCommand) cmdService;
		cmd.clear();
		cmd.setService("ssh");
		cmd.setTargetMachine(componentName);
		cmd.appendOption(" -c \"" + sshcmd + "\"");
		cmd.execute();
		return cmd;
	}

	public boolean isStaticAndReused(Component component) {
			return 
				component.isStatic() && isComponentReused(component);
	}

	public boolean isComponentReused(Component component) {
		return "true".equalsIgnoreCase(envProps.getProperty(component.getLocalizedName() + "_reused"));
	}

	public void execute() {
		LOG.severe("must be implemented");
	}

	public void verify() {
		LOG.severe("must be implemented");
	}

	public void markStarted() {
		environment.update(getPhase(), State.STARTED);
	}

	public void markCompleted() {
		environment.update(getPhase(), State.COMPLETED);
	}

	public void markError(VagrantException t, StatusCode code)
			throws VagrantException {
		environment.update(getPhase(), State.ERROR);
		environment.markError(t, code);
		throw t;
	}
}
