package com.dlouvton.badger.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MongoManager {
	private static Logger LOG = CustomLoggerFactory
			.getLogger(MongoManager.class);
	
	public static final String TEST_RESULTS_DB_NAME = "badger";
	public static final String TEST_RESULTS_COLLECTION_NAME = "main";
	public static final String USAGE_DB_NAME = "badger-usage";
	public static final String USAGE_COLLECTION_NAME = "environments";
	// choosing 25 for the maximum number of connections originating from one client
	private static final int connectionsPerHost = 25;
	// set these options to keep the connection alive for longer setups
	private static final boolean autoConnectRetry = true;
	private static final boolean socketKeepAlive = true;
	
	private static MongoClient mongo;
	private DBCollection collection;
	private BasicDBObject document;

	private static String mongoHost;

	/**
	 * constructor using the default collection name and default db name
	 * Note that we override some default values such as socketKeepAlive and autoConnectRetry
	 * @param mongoHost MongoDB host name
	 */
	public MongoManager(String mongoHost) {
			MongoManager.mongoHost = mongoHost;
			connect();
	}

	/**
	 * establish a connection with mongo DB
	 */
	private static void connect() {
		try {
			mongo = new MongoClient(mongoHost, new MongoClientOptions.Builder()
			.connectionsPerHost(connectionsPerHost)
			.socketKeepAlive(socketKeepAlive)
			.autoConnectRetry(autoConnectRetry).build());
		} catch (UnknownHostException e) {
			Utils.logSevere(e, "Mongo Host cannot be accessed");
		}
	}
	
	/**
	 * constructor for invocation by unit tests
	 * @param DBCollection the collection where documents are inserted
	 */
	public MongoManager(DBCollection collection) {
			this.collection = collection;
	}

	/**
	 * convert xml test results (usually testNG or xUnit files) to DBObject 
	 * @param xmlPath   path to xml file
	 * @patam additionalData    a map of key-value pairs to add to the collection generated from the xml
	 * @return DBObject  a DB Object representing the converted xml	 
	 */
	public BasicDBObject convertXmlResultsToMongoDocument(String xmlPath, Map<String, String> additionalData) throws IOException {
		try {
			String xmlAsText = Utils.fileToString(xmlPath);
			BasicDBObject xmlResultsObject = (BasicDBObject) com.mongodb.util.JSON.parse(MongoManager
					.toJSON(xmlAsText));
			if (additionalData != null) {
				xmlResultsObject.putAll(additionalData);
			}
			return xmlResultsObject;
		} catch (MongoException me) {
			Utils.logSevere(me, "Error converting test results to DBObject");
			return null;
		}
	}

	/**
	 * post a document to a database
	 * @param BasicDBObject a document to post
	 * postDocument(xmlResultsObject, TEST_RESULTS_DB_NAME, TEST_RESULTS_COLLECTION_NAME);
	 */
	public void postDocument (BasicDBObject documentToPost, String dbName, String collectionName) throws IOException {
		reconnect();
		try {
			document = documentToPost;
			collection = mongo.getDB(dbName).createCollection(collectionName, null);
			collection.insert(document);
			LOG.info(String.format("Posted document to mongoDB: dbName: %s, collectionName: %s; run_id: %s; docs count: %s", 
					dbName, collectionName, documentToPost.get("_run_id"), getCollection().find().count()));
		} catch (MongoException me) {
			Utils.logSevere(me, "Error posting results to MongoDB");
		}
	}
	
	/**
	 * update an existing document on the database, select a document with the same _run_id and the key
	 * @param key document key to match
	 * @param value new value
	 * @param documentToUpdate document to update
	 * @param dbName database name 
	 * @param collectionName collection name
	 */
	public void updateDocument(BasicDBObject newData, String dbName, String collectionName) throws IOException {
		reconnect();
		try {			
			collection = mongo.getDB(dbName).createCollection(collectionName, null);
			int numUpdates = collection.update(new BasicDBObject().append("_run_id", document.get("_run_id").toString()), newData).getN();
			LOG.info(String.format("Updated document on mongoDB: new data: %s, dbName: %s, collectionName: %s; run_id: %s; number updated: %s", 
					newData.toString(), dbName, collectionName, document.get("_run_id"), numUpdates));
		} catch (MongoException me) {
			Utils.logSevere(me, "Error updating document on MongoDB");
		}
	}

	/**
	 * reconnect to Mongo server, if the connection is lost
	 */
	private void reconnect() {
		try {
			mongo.getConnector().getDBPortPool(mongo.getAddress()).get().ensureOpen();
		} catch (Exception e) {
			LOG.warning("Lost database connection, will reconnect");
			connect();
		}
	}
	
	public void uploadFile(String path, String dbName, String runId) {
		try {
			GridFS fs = new GridFS(mongo.getDB(dbName));
			GridFSInputFile f = fs.createFile(new File(path));
			f.put("_run_id", runId);
			f.setContentType("text/html");
			f.save();
			LOG.info("Posted file " + path + " to GridFS");
		} catch (FileNotFoundException fnfe) {
			LOG.warning("File " + path + " could not be found, not uploading to GridFS ");
		} catch (Exception e) {
			Utils.logSevere(e, "Error uploading file " + path + " to GridFS ");
		}
	}
	
	/**
	 * returns a stream of a file on GridFS, retrieved by _run_id and filename
	 * @param dbName mongo database name
	 * @param runId select a document with this runId
	 * @param fileName select a document with a file name containing this name
	 * @return InputStream a stream that is being passed to the client
	 * @throws FileNotFoundException 
	 */
	public InputStream getFileByRunId(String dbName, String runId, String fileName) throws FileNotFoundException {
			GridFS fs = new GridFS(mongo.getDB(dbName));
			GridFSDBFile gridFile = fs.findOne(new BasicDBObject().append("_run_id", runId).append("filename", Pattern.compile(fileName)));
			if (gridFile != null) {
				 return gridFile.getInputStream();
			}
			throw new FileNotFoundException("File "+fileName+" with run id "+runId +" could not be found");
	}
	
	/**
	 * Returns the current collection  
	 * @return DBCollection  current collection
	 */
	public DBCollection getCollection() {
		return collection;
	}
	
	/**
	 * Returns the current document  
	 * @return DBObject  current document
	 */
	public DBObject getDocument() {
		return document;
	}
	
	/**
	 * Converting an xml string into json string 
	 * @param str xml string
	 * @return String containing json data
	 */
	public static String toJSON(String str) {
		try {
			XMLSerializer xmlSerializer = new XMLSerializer();
			JSON json = xmlSerializer.read(str);

			return json.toString(2).replaceAll("\"[@|#]\\s*(\\w+)\"", "_$1");
		} catch (Exception ex) {
			Utils.logSevere(ex, "Exception caused by incorrect/mismatched tags in the xml, Needs Fix : "
					+ ex.getMessage());
			return null;
		}
	}

}


	
