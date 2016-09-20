package com.dlouvton.badger.api.v1;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import com.dlouvton.badger.api.server.BadRequestException;
import com.dlouvton.badger.api.server.BadgerServerException;
import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.VagrantCommand;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.phases.PhaseImpl;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.util.PropertyLoader;
import com.dlouvton.badger.util.ShellCommand;
import com.dlouvton.badger.util.Utils;
import org.testng.TestNG;
import org.testng.TestNGException;
import org.testng.collections.Lists;

/**
 * Environment Resource.
 * This resource provides the ability to retrieve the provisioned environments and do some actions on them.
 * You can list the environments; view a specific environment (by ID); delete an environment.
 * You can also ssh into the environment in order to run a script, and get information about the components in the environment.
 * 
 */
@Path("/v1/environment")
@Produces({ MediaType.APPLICATION_JSON })
public class EnvironmentResource {

	public static final String SCRIPTS_DIR = "vagrant/post-install-scripts/";

	@GET
	public Response getEnvironmentList() throws BadRequestException {
		EnvironmentList environmentList;
		try {
			environmentList = new EnvironmentList(VagrantProvisioner
					.getInstance().getEnvironments());
		} catch (VagrantException e) {
			throw new BadRequestException("Failed to retrieve environment list");
		}
		return Response.ok(environmentList).build();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Path("/{id}")
	@GET
	public Response getOne(@PathParam("id") int environmentId) throws BadRequestException {
		Environment environment;
		try {
			environment = VagrantProvisioner.getInstance().getEnvironments()
					.get(environmentId);
			File environmentProperties = environment.getModel()
					.getEnvironmentProperties().getFile();
			// FIXIT, we have one env.properties, need to have one per env
			environment.setEnvProperties((Map) PropertyLoader
					.getPropertiesFromFile(environmentProperties));
		} catch (NullPointerException e) {
			throw new BadRequestException("Failed to retrieve environment "+environmentId);
		}
		return Response.ok(getEnvironment(environmentId)).build();
	}

	@Path("/{id}")
	@DELETE
	public Response teardown(@PathParam("id") int environmentId)
			throws BadgerServerException {
		final VagrantProvisioner provisioner = VagrantProvisioner.getInstance();
		Environment envToDestroy= provisioner.getEnvironments().get(environmentId);
		if (provisioner.environmentExists(envToDestroy) &&  envToDestroy.isStateEquals(State.BUSY)) {
			throw new BadgerServerException("Cannot destroy an environment while it's being provisioned. Please try later.");
		}
		
		try {
			VagrantProvisioner.performTeardown(true);
			provisioner.destroy(envToDestroy);
		} catch (VagrantException e) {
			throw new BadgerServerException("Failed to destroy environment. "+e.getMessage());
		}
		return getEnvironmentList();
	}

	@GET
	@Path("/{id}/component")
	public Response componentAction(@PathParam("id") int environmentId,
			@DefaultValue("list") @QueryParam("action") String action)
			throws VagrantException {
		// list is the default value. show an environment list
		if ("list".equalsIgnoreCase(action)) {
			VagrantProvisioner.getInstance().updateComponentsStatus();
			return Response.ok(getEnvironment(environmentId).componentsStatus).build();
		}

		VagrantProvisioner provisioner = VagrantProvisioner.getInstance();
		VagrantProvisioner.performTeardown(true);

		// destroy all components in all environments
		if ("force_destroy_all".equalsIgnoreCase(action)) {			
			try {
				provisioner.forceDestroyAll();
				return getEnvironmentList();
			} catch (VagrantException e) {
				throw new BadgerServerException(     
						"Failed to force destroy environments. "+e.getMessage());
			} catch (ResourceBusyException e) {
				return Response.status(500)
						.entity("Cannot destroy the environments while Provisioner is busy. Please try again later")
						.type("text/plain").build();
			}

		}

		// destroy the current model, including statics (unlike DELETE /environment/{id})
		if ("force_destroy_model".equalsIgnoreCase(action)) {
			try {
				provisioner.forceDestroyModel(getEnvironment(environmentId));
				return getEnvironmentList();
			} catch (VagrantException e) {
				throw new BadgerServerException(     
						"Failed to force destroy environment "+environmentId + "; "+e.getMessage());
			} catch (ResourceBusyException e) {
				return Response.status(500)
						.entity("Cannot destroy the environment while Provisioner is busy. Please try again later")
						.type("text/plain").build();
			}

		}
		throw new BadRequestException("Action " + action + " is not supported");
	}

	@Path("/{id}/plain-properties")
	@GET
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getPlainTextProperties(@PathParam("id") int environmentId)
			throws BadRequestException, FileNotFoundException {
		File environmentProperties;
		try {
			environmentProperties = getEnvironment(environmentId)
					.getModel().getEnvironmentProperties().getFile();

		} catch (NullPointerException e) {
			throw new BadRequestException("Failed to retrieve plain properties");
		}
		return Response.ok().entity(new FileInputStream(environmentProperties))
				.build();
	}

	private Environment getEnvironment(int environmentId) {
		return VagrantProvisioner.getInstance().getEnvironments()
				.get(environmentId);
	}

	@Path("/{id}/script")
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public FileNames getScripts()  {
		FileNames scripts = new FileNames();
		scripts.scripts = Utils.getFolderFileNames(SCRIPTS_DIR);
		return scripts;
	}

	@GET
	@Path("/{id}/script/{scriptName}")
	public Response runScript(@PathParam("id") int environmentId,
			@PathParam("scriptName") String scriptName,
			@DefaultValue("print") @QueryParam("action") String action,
			@DefaultValue("none") @QueryParam("component") String componentName)
			throws BadRequestException, IOException {

		if ("print".equalsIgnoreCase(action)) {
			return Response.ok(Utils.fileToString(SCRIPTS_DIR + scriptName))
					.build();
		}

		if ("none".equalsIgnoreCase(componentName)) {
			throw new BadRequestException("Component must be specified");
		}

		if ("run".equalsIgnoreCase(action)) {
			return ssh(appendEnvID(componentName,environmentId), "/" + SCRIPTS_DIR
					+ scriptName);
		}

		throw new BadRequestException("Action " + action + " is not supported");
	}

	@Path("/{id}/suite")
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public FileNames getSuites()  {
		FileNames suites = new FileNames();
		suites.scripts = Utils.getFolderFileNames("src/resources/suites");
		return suites;
	}

	@Path("/{id}/test")
	@GET
	public Response runSuiteMvn(@PathParam("id") int environmentId,
							 @DefaultValue("runTests") @QueryParam("action") String action)
			throws TestNGException {
		ShellCommand shell = new ShellCommand(new File("."),"TestNG");
		shell.setExecutable("./run.sh");
		shell.appendArg("-e "+environmentId+" test-via-api");
		shell.execute();
		if (shell.getExitValue()!= 0) {
			return Response.status(500)
					.entity(new Result(shell)).build();
		}
		return Response.ok().entity(new Result(shell)).build();
	}

	@GET
	@Path("/{id}/script/command")
	public Response runCommand(@PathParam("id") int environmentId,
			@QueryParam("command") String command,
			@DefaultValue("none") @QueryParam("action") String action,
			@DefaultValue("none") @QueryParam("component") String componentName)
			throws Throwable {
		if (!"run".equalsIgnoreCase(action)) {
			throw new BadRequestException("Action run must be specified");
		}
		return ssh(appendEnvID(componentName,environmentId), command);
	}

	private String appendEnvID(String componentName, int id) {
		return componentName+"__"+id;
	}

	public Response ssh(String componentName, String command) throws VagrantException{
		try {
			Result result = new Result(PhaseImpl.ssh(componentName, command, VagrantProvisioner.getInstance().getCommandLineService()));
			return Response.ok(result).build();
		} catch (VagrantException e) {
			throw new BadgerServerException("failed to ssh and run command "+command+" against "+componentName+". "+e.getMessage());
		}
	}

	@XmlRootElement
	static class EnvironmentPropertiesMap {
		public Map<String, String> envProperties;
	}

	@XmlRootElement
	static class EnvironmentList {
		public List<String> environments;
		public int numberOfEnvironments;

		private EnvironmentList(Map<Integer, Environment> map) {
			if (map == null) {
				numberOfEnvironments = 0;
				environments = null;
				return;
			}
			environments = new ArrayList<String>();
			for (Environment env : map.values()) {
				StringBuffer buffer = new StringBuffer("ID: "
						+ env.environmentId);
				buffer.append("; Model: " + env.modelName);
				buffer.append("; Model File: " + env.modelFileName);
				buffer.append("; Status: " + env.environmentStatus);
				environments.add(buffer.toString());
			}
			numberOfEnvironments = environments.size();
		}

		private EnvironmentList() {

		}
	}

	@XmlRootElement
	static class Result {
		public int exitCode;
		public String stderr;
		public String stdout;

		Result() {
		}

		Result(ShellCommand cmd) {
			exitCode = cmd.getExitValue();
			stderr = cmd.getStderr();
			stdout = cmd.getStdout();
		}
	}

	@XmlRootElement
	static class FileNames {
		public List<String> scripts;
		public String scriptsLocation = SCRIPTS_DIR;
	}
	
	

}
