package com.dlouvton.badger.provisioner.phases;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.dlouvton.badger.provisioner.*;
import com.dlouvton.badger.provisioner.model.Component;
import com.dlouvton.badger.provisioner.model.EnvironmentProperties;
import com.dlouvton.badger.provisioner.status.Phase;
import com.dlouvton.badger.util.ShellCommand;

public class PuppetInstaller extends PhaseImpl {

	public PuppetInstaller(Environment environment) {
		super(environment);
	}

	private final static int DEFAULT_INSTALL_TIMEOUT_MINUTES = 30;
	private List<VagrantCommand> commands;
	private boolean exited;

	private int getInstallTimeoutMinutes() {
		if (model.getModelParameters().containsKey("installTimeoutMinutes")) {
			return Integer.parseInt(model.getModelParameters().get(
					"installTimeoutMinutes"));
		}

		return DEFAULT_INSTALL_TIMEOUT_MINUTES;
	}

	public void execute() {
		LOG.info("Installing environment (timeout: "
				+ this.getInstallTimeoutMinutes() + " minutes)");
		boolean singleThreadedExecution = Boolean.valueOf(model.getModelParameter("single-thread-puppet-installation", properties
				.getProperty("single-thread-puppet-installation")));
		
		List<Component> components = model.getComponents();
		try {
			ExecutorService es;
			if (singleThreadedExecution) {
				es = Executors.newSingleThreadExecutor();
			} else {
				es = Executors.newFixedThreadPool(components.size());
			}

			commands = new ArrayList<VagrantCommand>();

			// Kick them all off in background threads
			for (Component component : components) {
				// check if we need to reinstall static components
				if (isStaticAndReused(component) && Boolean.valueOf(properties.getProperty("reinstall-static-components")) == false) {
					LOG.info("Skipping installation for component "+component.name);
					continue;
				}
					
				cmd = newCommand(component.name);
				cmd.setService("provision");
				cmd.setTargetMachine(component.name);
				cmd.setProvisioner("puppet");
				commands.add(cmd);
				es.execute(cmd);
			}

			// Wait for them all to be done
			es.shutdown();
			exited = es.awaitTermination(this.getInstallTimeoutMinutes(),
					TimeUnit.MINUTES);

			verify();

			// Add entries for each component into /etc/hosts on the machine
			// that is running the tests. Use puppet to accomplish that.
			// Do this only on aws.
			// There is surely a better place and manner to do this...
			for (Component component : components) {
				if (component.getProvider().equals("aws"))
					addEtcHostsEntry(component);
			}
		} catch (VagrantException e) {
			throw new VagrantException(
					"Error while running the 'install' phase", e);
		} catch (InterruptedException ie) {
			throw new VagrantException(
					"Interrupted while running the 'install' phase", ie);
		}
	}

	public void verify() {
		if (!exited) {
			throw new VagrantException("Timeout exceeded in 'install' phase!");
		}

		for (VagrantCommand cmd : commands) {
			if (cmd.getExitValue() != SUCCESS) {
				throw new VagrantException(
						"components are not installed properly");
			}
		}
	}

	public void addEtcHostsEntry(Component component) {
		EnvironmentProperties env = model.getEnvironmentProperties();
		String hostname = env.getProperty(component.getLocalizedName() + "_HostName");
		String ip = env.getProperty(component.getLocalizedName() + "_IPAddress");

		if (hostname == null || ip == null || hostname.length() == 0
				|| ip.length() == 0) {
			throw new RuntimeException(
					"Don't know the values for the hostname and IP of component "
							+ component.name + "!");
		}

		// Desired command line:
		// sudo puppet apply -e ' host { "localhost": name => "${hostname}", ip
		// => "${ipaddress}", ensure => present, }'
		ShellCommand command = new ShellCommand(model.getWorkingDirectory());
		command.setExecutable("sudo");
		command.appendArg("puppet apply -e ' host {");
		command.appendArg("\"" + hostname + "\"");
		command.appendArg(": name => ");
		command.appendArg("\"" + hostname + "\"");
		command.appendArg(", ip => ");
		command.appendArg("\"" + ip + "\"");
		command.appendArg(", ensure => present, }' ");

		command.execute();
	}

	public Phase getPhase() {
		return Phase.installation;
	}
}
