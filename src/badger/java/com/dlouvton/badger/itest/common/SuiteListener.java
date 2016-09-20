package com.dlouvton.badger.itest.common;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Configuration;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.util.CustomLoggerFactory;
import com.dlouvton.badger.util.PropertyLoader;

@SuppressWarnings("deprecation")
public class SuiteListener implements ISuiteListener {
	private VagrantProvisioner provisioner;
	public static final long NULL_ENV_ID = 0;
	private final Properties properties;
	private int envID;
	private boolean isApiUser = true;
	Environment testbed;
	private final Logger LOG = CustomLoggerFactory
			.getLogger(SuiteListener.class);

	public SuiteListener() {
		properties = PropertyLoader.getTopLevelProperties();
		envID = getEnvID();
		LOG.info("Loading properties; using envID " + envID + (isApiUser ? " (api user)" : ""));
	}

	private int getEnvID() {
		try {
			return Integer.parseInt( System.getProperty("envID") );
		}
		catch( NumberFormatException e ) {
			isApiUser = false;
			return 	Integer.valueOf(properties.getProperty("environment-id"));
		}
	}

	/**
	 * when suite starts - perform test bed setup
	 * @param suite test suite
	 */
	public void onStart(ISuite suite) {
		try {
			suite.getXmlSuite().setName("Badger");
			provisioner = VagrantProvisioner.getInstance();
		} catch (VagrantException e) {
			throw new SetupException("Error creating VagrantProvisioner!", e);
		}

		if (!properties.containsKey("model-path"))
			throw new SetupException(
					"Property model-path is required, and was not present in"
							+ " default.properties or user.properties.");

		String modelPath = properties.getProperty("model-path");
		try {
			if ("false".equalsIgnoreCase(properties.getProperty(
					"perform-setup", "true")) || isApiUser) {
				LOG.info("Not performing setup. Using environment "+ envID);
				properties.put("provision-time", "0 sec");
				testbed = provisioner.getEnvironment(envID);
				suite.setAttribute("_run_id", testbed == null ? NULL_ENV_ID : testbed.getIdentifier());
				return;
			}
			testbed = provisioner.init(new Model(new File(modelPath), envID));
			suite.setAttribute("_run_id", testbed.getIdentifier());
			provisioner.setup(testbed, Boolean.valueOf(properties.getProperty(
					"perform-teardown-on-setup-failure", "true")));
		} catch (SetupException e) {
			throw new SetupException(
					"SuiteListener: Failed to setup suite from model "
							+ modelPath + ", aborting.", e);
		} catch (ResourceBusyException e) {
			throw new SetupException(
					"The Provisioner is busy serving other requests. Please try again later.", e);
		}
	}

	/**
	 * when completing a test suite, update the env, and perform teardown
	 * @param suite test suite
	 */
	@AfterSuite(alwaysRun = true)
	@Configuration
	public void onFinish(ISuite suite) {
		if (testbed != null) {
			//there should be one result set
			for (ISuiteResult sr : suite.getResults().values()) {
				testbed.numFailedTests = sr.getTestContext().getFailedTests().size();	
				testbed.numPassedTests = sr.getTestContext().getPassedTests().size();
				testbed.testSuiteName = sr.getTestContext().getName();
				testbed.testEndTime = String.valueOf(sr.getTestContext().getEndDate().getTime());						
			}
			testbed.updateWithTestStats();
		}
		
		removeSkippedResults(suite);		
		teardown();
	}

	/**
	 * teardown environment - destroy dynamic components
	 * @param None
	 */
	private void teardown() {
		boolean performTeardown = Boolean.valueOf(properties.getProperty(
				"perform-teardown", "true"));
		try {			
			if (!performTeardown || isApiUser) {
				LOG.info("Not performing teardown");
			} else {
				LOG.info("Tearing down environment");
				provisioner.destroy(testbed);
				LOG.info("Teardown completed successfully");
			}
		} catch (SetupException e) {
			throw new SetupException("SuiteListener: Failed to destroy model");
		}
	}

	/**
	 * clean up the html and xml reports -  remove skipped results
	 * @param suite xml suite
	 */
	
	private void removeSkippedResults(ISuite suite) {
		if (Boolean.valueOf(properties.getProperty("remove-skipped-results","true"))) {
			for (ISuiteResult sr : suite.getResults().values()) {
				ITestContext testContext = sr.getTestContext();

				for (ITestResult tr: testContext.getSkippedTests().getAllResults()) {
					testContext.getSkippedTests().removeResult(tr.getMethod());
				}
				for (ITestResult tr: testContext.getSkippedConfigurations().getAllResults()) {
					testContext.getSkippedConfigurations().removeResult(tr.getMethod());
				}
			}
		}
	}
}
