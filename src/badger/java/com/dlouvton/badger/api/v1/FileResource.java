package com.dlouvton.badger.api.v1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.BasicDBObject;
import com.dlouvton.badger.api.server.BadgerServerException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.util.MongoManager;
import com.dlouvton.badger.util.PropertyLoader;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
/*
 * File Resource.
 * This resource provides the ability to upload files onto the Badger server (Host), MongoDB, GridFS.
 * In order to use it you need to post a multi-part form. Look for example at the test BadgerApiTest.testFileUpload() 
 * When uploaded onto MongoFS, the file has to be an xml (with TestNG format), so it can be parsed and unloaded as key-value pairs.
 * When uploaded onto GridFS, the file will be persisted on the GridFS storage.
 * When uploaded onto the host, the file will be kept under <badger-home>/target.
 */
@Path("/v1/file")
public class FileResource {

	private static final String SERVER_UPLOAD_LOCATION_FOLDER = "target/";
	MongoManager mongo = VagrantProvisioner.getMongoManager(); 

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
			@DefaultValue("host") @QueryParam("destination") String destination)
			throws Throwable {
		
		String filePath = null;
		filePath = SERVER_UPLOAD_LOCATION_FOLDER
				+ contentDispositionHeader.getFileName();
		Properties props = PropertyLoader.getTopLevelProperties();
		
		// save the file to the server
		saveFile(fileInputStream, filePath);
		try {
			switch (destination) {
			case "mongodb":
				BasicDBObject document = mongo.convertXmlResultsToMongoDocument(filePath, null);
				mongo.postDocument(document, MongoManager.TEST_RESULTS_DB_NAME, props.getProperty("mongo-collection"));
				break;
			case "gridfs":
				mongo.uploadFile(filePath, MongoManager.TEST_RESULTS_DB_NAME, String.valueOf(System.currentTimeMillis()));
				break;
			case "host":
				// do nothing with the file; leave it on host
				break;
			default:
				throw new IllegalArgumentException("Invalid destination: "
						+ destination);
			}
		} catch (IOException ioe) {
			throw new BadgerServerException("failed to upload file "+ filePath+ " to " + destination);
		} catch (IllegalArgumentException iae) {
			throw new BadgerServerException(iae.getMessage());
		}
		
		return Response.status(200).entity("File " + filePath + " was uploaded successfully to "
				+ destination).build();
	}

	/**
	 * returns file from GridFS, retrieved by filename and additional identifier (currently _run_id)
	 * @param dbName mongo database name
	 * @param id select a document where identifier equals this id
	 * @param idField identifier field name 
	 * @param fileName select a document with this file name
	 * @return Response a stream that is being passed to the client
	 */
	@GET
	@Path("/{id}")
	public Response getFile(@PathParam("id") String id,
			@DefaultValue("run_id") @QueryParam("idField") String idField,
			@DefaultValue("html") @QueryParam("filename") String filename,
			@DefaultValue("badger") @QueryParam("dbName") String dbName) {
		try {
			return Response.ok().entity(mongo.getFileByRunId(dbName, id, filename)).build();
		} catch (FileNotFoundException e) {
			return Response.status(404)
					.entity(e.getMessage())
					.type("text/plain").build();
		} 		
	}
	
	/**
	 * returns raw file from GridFS, retrieved by filename and additional identifier (currently _run_id)
	 * @param dbName mongo database name
	 * @param id select a document where identifier equals this id
	 * @param idField identifier field name 
	 * @param fileName select a document with this file name
	 * @return Response raw stream that is being passed to the client (text/plain)
	 */
	@GET
	@Path("/{id}/plain")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getPlainTextFile(@PathParam("id") String id,
			@DefaultValue("run_id") @QueryParam("by") String by,
			@DefaultValue("log") @QueryParam("filename") String filename,
			@DefaultValue("badger") @QueryParam("dbName") String dbName) {
		return getFile(id, by, filename, dbName);	
	}
	
	// save uploaded file to a defined location on the server
	private void saveFile(InputStream uploadedInputStream, String serverLocation)
			throws IOException {
		OutputStream outpuStream = new FileOutputStream(
				new File(serverLocation));
		int read = 0;
		byte[] bytes = new byte[1024];

		outpuStream = new FileOutputStream(new File(serverLocation));
		while ((read = uploadedInputStream.read(bytes)) != -1) {
			outpuStream.write(bytes, 0, read);
		}
		outpuStream.flush();
		outpuStream.close();
	}
	
	

}

