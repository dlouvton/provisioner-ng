package com.dlouvton.badger.itest.common;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import com.mongodb.BasicDBObject;
import com.dlouvton.badger.ATAM.ContextProvider;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.util.CustomLoggerFactory;
import com.dlouvton.badger.util.MongoManager;
import com.dlouvton.badger.util.PropertyLoader;

@Listeners(com.dlouvton.badger.itest.common.SuiteListener.class)
public class BaseTest implements ContextProvider {

	protected static ITestContext context;
	public static final String TESTNG_REPORT_NAME="emailable-report.html";

	// any test can use this logger to log INFO, SEVERE, FINE, WARNING messages
	public Logger LOG = CustomLoggerFactory.getLogger(getClass());

	// the context stores information about the components and any other
	// information related to the tests
	public ITestContext getContext() {
		return context;
	}

	public String get(String key) {
		if (context.getAttribute(key) == null) {
			if (Boolean.valueOf(get("skip-not-supported-tests"))) {
				throw new BadgerSkipException(
						"SKIPPED: the test does not support the parameter '"
								+ key + "'");
			} else {
				return "";
			}
		}

		return context.getAttribute(key).toString();
	}

	@BeforeSuite(alwaysRun = true)
	public void beforeSuite(ITestContext ctx) throws IOException {
		context = ctx;
		populateITestContextWithRoles(context);
	}

	@AfterSuite(alwaysRun = true)
	public void afterSuite(ITestContext context) throws IOException {
		if (Boolean.valueOf(get("post-to-mongo"))) {
			String out = context.getOutputDirectory() + "/" + context.getName();
			LOG.info("Uploading results from " + out + " to MongoDB");
			MongoManager mongo = VagrantProvisioner.getMongoManager();
			BasicDBObject document = mongo.convertXmlResultsToMongoDocument(out + ".xml", additionalSuiteData());
			String runId = String.valueOf(context.getSuite().getAttribute("_run_id"));
			document.put("_run_id",runId);
			mongo.postDocument(document, MongoManager.TEST_RESULTS_DB_NAME, context.getName());
			if (Boolean.valueOf(get("post-files-to-mongo"))) {
				mongo.uploadFile(context.getOutputDirectory()+"/../"+TESTNG_REPORT_NAME, MongoManager.TEST_RESULTS_DB_NAME, runId);
				mongo.uploadFile("out.log", MongoManager.TEST_RESULTS_DB_NAME, runId);
			}
		} else {
			LOG.info("Skipped uploading results to MongoDB");
		}
	}

	public Map<String, String> additionalSuiteData() {
		Map<String, String> data = new HashMap<String, String>();
		for (String field : get("additional-fields-to-post").split(",")) {
			if (!field.isEmpty()) {
				// dash in front of the field name tells the clients
				// (i.e. mongo driver) that it's a simple field and not an
				// array.
				data.put("_" + field.replaceAll("-", "_"), get(field));
			}
		}

		return data;
	}

	private void loadPropertiesOntoContext(Properties props) {
		Enumeration<?> en = props.propertyNames();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			context.setAttribute(key, props.getProperty(key));
		}
	}

	private void logEnvironmentProperties() {
		LOG.fine("**************************************");
		LOG.fine("*** All properties available to tests:");
		for (String s : context.getAttributeNames()) {
			LOG.fine(s + " = " + context.getAttribute(s));
		}
		LOG.fine("**************************************");
	}

	protected static String httpGet(String url) throws ClientProtocolException,
			IOException {
		return Request.Get(url).execute().returnContent().asString();
	}

	public void populateITestContextWithRoles(ITestContext context) {
		// Put defaults.properties and user.properties onto context
		loadPropertiesOntoContext(PropertyLoader.getTopLevelProperties());

		// Also put the stuff from environment.properties onto context
		// However, that lives in the workingdir specified in the model file
		// Follow the chain to load those properties in
		String modelPath = this.get("model-path");
		Model model = new Model(new File(modelPath),
				Integer.valueOf(get("environment-id")));
		File environmentProperties = model.getEnvironmentProperties().getFile();
		Properties envProps = PropertyLoader
				.getPropertiesFromFile(environmentProperties);
		loadPropertiesOntoContext(envProps);

		// Dump the environment properties to command line
		logEnvironmentProperties();
	}
}
