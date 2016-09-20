package com.dlouvton.badger.provisioner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment.ComponentsStatus;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.phases.InstanceDestroyer;
import com.dlouvton.badger.provisioner.phases.PhaseImpl;
import com.dlouvton.badger.provisioner.status.Phase;
import static com.dlouvton.badger.provisioner.status.State.*;
import com.dlouvton.badger.provisioner.status.Status;
import com.dlouvton.badger.provisioner.status.StatusCode;
import com.dlouvton.badger.util.CustomLoggerFactory;
import com.dlouvton.badger.util.MongoManager;
import com.dlouvton.badger.util.PropertyLoader;
import com.dlouvton.badger.util.ShellCommand;
import com.dlouvton.badger.util.Utils;

/**
 * This is the Vagrant implementation of the Provisioner interface.
 * It's a singleton and provides Vagrant implementations to init, setup and teardown.
 * When one inits a model, an Environment is returned.
 * Then setup boots up an environment, and teardown destroys it. 
 */
public class VagrantProvisioner implements Provisioner {

	private static final Logger LOG = CustomLoggerFactory
			.getLogger(VagrantProvisioner.class);
	private ShellCommand cmdlineService;
	private static VagrantProvisioner instance = null;
	public final static int MAX_NUM_ENVIRONMENTS = 2;
	private static Map<Integer, Environment> environments;
	private Environment currentEnvironment = null;
	private static MongoManager mongoManager;
	private static Status status = null;
	private static Properties properties = PropertyLoader
			.getTopLevelProperties();
	public static boolean performSetup;
	public static boolean performTeardown;
	private PhaseImpl currentPhase;

	// Private because it's a singleton
	private VagrantProvisioner() {
	}

	public static synchronized VagrantProvisioner getInstance()
			throws VagrantException {
		if (instance == null) {
			try {
				instance = new VagrantProvisioner();
				status = new Status();
				mongoManager = new MongoManager(properties.getProperty("mongo-host"));
				performSetup = Boolean.valueOf(properties.getProperty(
						"perform-setup", "true"));
				performTeardown = Boolean.valueOf(properties.getProperty(
						"perform-teardown", "true"));
				environments = new HashMap<Integer, Environment>();
			} catch (VagrantException e) {
				LOG.severe("Could not start the Vagrant provisioner!");
				e.printStackTrace();
				throw new VagrantException(
						"Could not start the Vagrant provisioner!", e);
			}
		}

		return instance;
	}

	public void setProvisionerService(ShellCommand service) {
		cmdlineService = service;
	}

	public ShellCommand getCommandLineService() {
		return cmdlineService;
	}

	// if the provisioner isn't busy, create a new environment for a model
	public Environment init(Model model) throws SetupException,
			ResourceBusyException {
		// throws ResourceBusyException if Provisioner is busy
		validateThatProvisionerIsNotBusy();
		if (cmdlineService == null) {
			// set the provisioner service once only
			setProvisionerService(new VagrantCommand(
					model.getWorkingDirectory()));
		}
		currentEnvironment = new Environment(model);
		LOG.info("Using model " + model.getName() + "; model path: "+ model.getFileName());
		executePhase(Phase.initialization, currentEnvironment);
		environments.put(currentEnvironment.environmentId, currentEnvironment);
		return currentEnvironment;
	}

	// bring up the environment, without installation and configuration
	public void up() throws VagrantException {
		executePhase(Phase.vm_creation, currentEnvironment);
	}

	// use the vagrant puppet provisioner to install software on the VMs;
	// expects exit value 0
	public void install() throws VagrantException {
		executePhase(Phase.installation, currentEnvironment);
	}

	// runs shell provisioner
	public void configure() throws VagrantException {
		executePhase(Phase.configuration, currentEnvironment);
	}

	
	/**
	 * destroys dynamic components (non-static)
	 * @param Environment environment to destroy
	 */
	public void destroy(Environment environment) throws VagrantException {
		if (performTeardown && environmentExists(environment)) {
			executePhase(Phase.teardown, environment);
			// note that the static components became unmanaged
			environments.remove(environment.environmentId);
		} else {
			LOG.fine(String.format("Not Performing teardown (performTeardown: %s, environment exists: %s)", performTeardown, environmentExists(environment)));
		}
		
	}

	
	/**
	 * destroys all components in all environments including static. This should
	 * note: this should only be called from API
	 * @param None
	 */
	public void forceDestroyAll() throws VagrantException, ResourceBusyException {
		if (performTeardown) {
			validateThatProvisionerIsNotBusy();
			LOG.info("Destroying all components in all environments");
			InstanceDestroyer.destroyAll();
			Utils.removeAllEntriesFromMap(environments);
		}
	}

	/**
	 * destroys all components in current model including static.
	 * @param Environment environment to destroy
	 */
	public void forceDestroyModel(Environment environment) throws VagrantException, ResourceBusyException {
		if (performTeardown && environmentExists(environment)) {
			validateThatProvisionerIsNotBusy();
			LOG.info("Destroying all components in environment "+environment.environmentId);
			new InstanceDestroyer(environment).destroyAllModelComponents();
			environments.remove(environment.environmentId);
		}
	}

	/**
	 * executes the appropriate Phase Implementation 
	 * e.g. given Phase.vm_creation will return InstanceCreator
	 * @param phase Phase to execute
	 * @param environment Environment to execute upon
	 * @return PhaseImpl the phase implementation after the execution
	 */
	private PhaseImpl executePhase(Phase phase, Environment environment)
			throws VagrantException {
		currentPhase = PhaseBuilder.runPhase(phase, environment);
		return currentPhase;
	}

	public void setup(Environment environment, boolean destroyOnError)
			throws SetupException {
		// Abort setup early if requested
		boolean destroyedOnError = false;
		try {
			currentEnvironment = environment;
			if (!performSetup) {
				LOG.info("skipping setup, due to properties!");
				currentEnvironment.markReused();
				currentEnvironment.updateData();
				properties.put("provision-time", "0 sec");
				return;
			}
			// TODO: remove the validateThatProvisionerIsNotBusy check when we support multiple concurrent environments
			validateThatProvisionerIsNotBusy();
			attachShutDownHook();
			LOG.info("Starting suite setup (Teardown on failure = "
					+ Boolean.toString(destroyOnError) + ")");
			currentEnvironment.setState(BUSY);
			up();
			install();
			configure();
			currentEnvironment.setupDuration = Utils.formatMilliseconds(System
					.currentTimeMillis() - environment.startTime);
			properties.put("provision-time", environment.setupDuration);
			LOG.info("Suite setup took " + environment.setupDuration
					+ " and completed successfully.");
			currentEnvironment.markReady();

		} catch (VagrantException e) {
			LOG.severe("Setup failed with the exception: " + e.getMessage());
			e.printStackTrace();
			currentEnvironment.markError(e, StatusCode.SystemError);
			if (destroyOnError) {
				LOG.severe("Destroying environment "
						+ currentEnvironment.environmentId);
				destroyedOnError = true;
			}
			throw new SetupException(
					"Setup failed: Failed to setup suite from model "
							+ environment.getModel().getName() + ", aborting.", e);
		} catch (ResourceBusyException e) {
			throw new SetupException(
					"Provisioner is Busy serving other requests. Please try again later.", e);
		} finally {
			if (performSetup) {
				updateComponentsStatus();
				currentEnvironment.updateData();
			}
			if (destroyedOnError) {
				destroy(currentEnvironment);
			}
		}
	}

	/**
	 * attaching a shutdown hook to handle setups that are interrupted
	 */
	private void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (INITIALIZED.name().equals(currentEnvironment.environmentStatus) || BUSY.name().equals(currentEnvironment.environmentStatus) ) {
					LOG.fine("Shutdown Hook Triggered, marking environment as interrupted");
					currentEnvironment.markInterrupted();
					currentEnvironment.updateData();
				}
			}
		});
		LOG.fine("Shutdown Hook Attached, environment state is: "+currentEnvironment.environmentStatus);
	}

	// gets the real vagrant status and updates the current environment
	public void updateComponentsStatus() {
		currentEnvironment.componentsStatus = new ComponentsStatus(
				currentPhase.getVagrantComponentsStatus());
	}

	// override properties in unit tests
	public static void performSetup(boolean bool) {
		performSetup = bool;
	}

	// override properties in unit tests
	public static void performTeardown(boolean bool) {
		performTeardown = bool;
	}

	// returns "global" status
	public Status getStatus() {
		return status;
	}

	public Map<Integer, Environment> getEnvironments() {
		return environments;
	}

	public void validateThatProvisionerIsNotBusy() throws ResourceBusyException {
		if (getEnvironments().isEmpty()) {
			return;
		}
		for (Environment env : getEnvironments().values()) {
			if (env.isStateEquals(ERROR)) {
				LOG.warning("Current Environment has errors. Destroying it.");
				destroy(env);
			}
			if (env.isStateEquals(BUSY)) {
				String msg = "Busy performing setup for environment "
						+ env.environmentId
						+ ". Cannot serve another setup request. Try again later.";
				LOG.warning(msg);
				throw new ResourceBusyException(msg);
			}
		}
	}

	/**
	 * return an Environment specified by Environment ID
	 * @param envID Environment ID
	 * @return Environment or null if does not exist
	 */
	public Environment getEnvironment(int envID) {
		
		if (environments.isEmpty()) {
			return null;
		}
		if (environments.containsKey(envID))  { 
			return environments.get(envID);
		}
		return null;
	}
	
	/**
	 * checks if environment exists
	 * @param Environment 
	 * @return true if exists
	 */
	public boolean environmentExists(Environment environment) {		
		return environments.containsValue(environment);
	}

	/**
	 * Get the Mongo Manager, that can establish connections to MongoDB
	 * @return MongoManager a database manager object
	 */
	public static MongoManager getMongoManager() {
		return mongoManager;
	}

}
