package com.dlouvton.badger.api.v1;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

/**
 * A resource class for Badger models. This resource allows to view a list of
 * available models, view specific models, and start a model.
 */
@Path("/v1/model")
@Produces({ MediaType.APPLICATION_JSON })
public class ModelResource {

	private static final String MODELS_DIR = "models/";
	private final static int THREAD_POOL_COUNT = 1;
	private final static ExecutorService executorService = Executors
			.newFixedThreadPool(THREAD_POOL_COUNT);
	private final VagrantProvisioner provisioner = VagrantProvisioner
			.getInstance();

	@GET
	public ModelNames getAll() {
		ModelNames models = new ModelNames();
		models.modelNames = Utils.getFolderFileNames(MODELS_DIR);
		return models;
	}

	@GET
	@Path("/{name}")
	public Response get(@PathParam("name") String modelName,
			@DefaultValue("list") @QueryParam("action") String action,
			@DefaultValue("-1") @QueryParam("envID") final int envID)
			throws BadgerServerException, IOException, ResourceBusyException {

		if ("list".equalsIgnoreCase(action)) {
			return Response.ok(
					Utils.fileToString(MODELS_DIR + modelName).replaceAll(
							"[\t]+", "\n"), MediaType.APPLICATION_JSON).build();
		}

		if ("start".equalsIgnoreCase(action)) {
			if (envID == -1) {
				throw new BadRequestException("Parameter envID is missing");
			}
			if (provisioner.getEnvironments().containsKey(envID) && provisioner.getEnvironment(envID).isStateEquals(State.READY)) {
				return Response.status(500)
						.entity("Environment " + envID + " is running. To start model "+modelName + " from scratch, please destroy the environment first. ")
						.type("text/plain").build();
			}

			try {
				provisioner.init(new Model(new File(MODELS_DIR + modelName),
						envID));
			} catch (SetupException e) {
				throw new BadgerServerException("Failed to initialize model "
						+ modelName + "; " + e.getMessage());
			}

			executorService.execute(new Runnable() {
				public void run() {
					VagrantProvisioner.performSetup(true);
					try {
						provisioner.setup(VagrantProvisioner.getInstance()
								.getEnvironments().get(envID), true);
					} catch (SetupException e) {
						e.printStackTrace();
					} // TODO: add "destroyOnError" option as param
				}
			});
			// TODO more generic way to handle unit testing.
			// this model is used for unit testing
			if (modelName.contains("a-b-c-d.json")) {
				provisioner.setProvisionerService(new FakeCommandLine());
				FakeCommandLine.addStdoutRule("verification", "4");
			}
			Environment env = VagrantProvisioner.getInstance()
					.getEnvironments().get(envID);
			env.user = env.user +" (api)";
			return Response.ok(env).build();
		}
		throw new BadRequestException("Action " + action + " is not supported");

	}

	@XmlRootElement
	static class ModelNames {
		public List<String> modelNames;
	}
}
