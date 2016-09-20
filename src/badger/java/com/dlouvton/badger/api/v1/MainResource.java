package com.dlouvton.badger.api.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dlouvton.badger.api.server.BadRequestException;
import com.dlouvton.badger.api.server.BadgerServerException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.status.Status;
import com.dlouvton.badger.util.Utils;

/**
 * provisioner/global status
 */
@Path("/v1/main")
public class MainResource {
	private static final String LOG_FILE_NAME = "out.log";

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Status getStatus() throws Exception {
		Status status = VagrantProvisioner.getInstance().getStatus();
		if (status == null) {
			return Status.notInitialized();
		}
		return status;
	}

	@GET
	@Path("/about")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response about (@DefaultValue("version") @QueryParam("action") String action)
			throws BadRequestException, IOException {

		if ("version".equalsIgnoreCase(action)) {
			return getVersion();
		}
		
		if ("log".equalsIgnoreCase(action)) {
			try {
				InputStream fis = new FileInputStream(new File(LOG_FILE_NAME));
				return Response.ok().entity(fis).build();
			} catch (IOException e) {
				throw new BadgerServerException("cannot retrieve log file "
						+ LOG_FILE_NAME);
			}
		}

		throw new BadRequestException(action + " is not a valid option");
	}


	private Response getVersion() throws IOException {
		StringBuffer buffer = new StringBuffer();
		Enumeration<URL> resources = getClass().getClassLoader()
				.getResources("META-INF/MANIFEST.MF");
		while (resources.hasMoreElements()) {
			try {
				Manifest manifest = new Manifest(resources.nextElement().openStream());
				Attributes attr = manifest.getMainAttributes();
				if (attr.getValue("Implementation-Vendor-Id") == null || ! attr.getValue("Implementation-Vendor-Id").equalsIgnoreCase("com.dlouvton.badger")) {
					continue;
				}
				buffer.append(getManifestAttributes(attr));
			} catch (IOException E) {
				throw new BadgerServerException("cannot retrieve Jar manifests");
			}
		}
		return Response.ok().entity(buffer.toString()).build();
	}

	private static String getManifestAttributes(Attributes attributes){
		Iterator it = attributes.keySet().iterator();
		final String SEPARATOR = " /";
		StringBuffer buffer = new StringBuffer();
		while (it.hasNext()){
			java.util.jar.Attributes.Name key = (java.util.jar.Attributes.Name) it.next();
			Object value = attributes.get(key);
			buffer.append(key + ":  " + value + SEPARATOR);
		}
		return buffer.toString();
	}
}
