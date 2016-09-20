package com.dlouvton.badger.test;

import static com.dlouvton.badger.util.FakeCommandLine.addStdoutRule;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.phases.InstanceCreator;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.provisioner.status.StatusCode;
import com.dlouvton.badger.tracker.MongoTracker;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

public class TrackerTest {

	VagrantProvisioner provisioner;
	Environment environment;
	// a count of the public fields on Environment.java that are initialized and not null 
	private static final String VAGRANT_ERROR_MESSAGE="not_installed";

	
	@Before
	public void setup() throws SetupException, ResourceBusyException  {
		Utils.removeAllEntriesFromMap(VagrantProvisioner.getInstance().getEnvironments());
		FakeCommandLine.clearRules();
		provisioner = VagrantProvisioner.getInstance();
		provisioner.setProvisionerService(new FakeCommandLine());
		addStdoutRule("vagrant -v", "Vagrant 1.6.3");
		VagrantProvisioner.performSetup(true);
		VagrantProvisioner.performTeardown(true);		
		environment = provisioner.init(new Model(new File("models/a-b-c-d.json"),1));
		addStdoutRule("verification", "4");
	}
	
	@Test
	public void testContainsFieldsIncludingRunId() {
		provisioner.setup(environment,true);
		assertTrue(environment.getData().containsField("_run_id"));
		assertTrue(environment.getData().containsField("modelName"));
		assertTrue(environment.getData().containsField("componentsStatus"));
	}
	
	@Test
	public void testAllFieldsAreNotNull() {
		provisioner.setup(environment,true);
		for (String field: environment.getData().keySet()) {
			assertNotNull(getFromTracker(field));
		}
		assertEquals(getFromTracker("environmentStatus"),State.READY.name());

	}
	
	@Test
	public void testSkipped() {
		VagrantProvisioner.performSetup(false);
		VagrantProvisioner.performTeardown(false);
		provisioner.setup(environment,true);
		assertEquals(getFromTracker("environmentStatus"),State.SKIPPED.name());
	}
	
	@Test
	public void testReady() {
		provisioner.setup(environment,true);
		assertEquals(getFromTracker("setupResult"),State.READY.name());
		assertEquals(getFromTracker("environmentStatus"),State.READY.name());
		assertEquals(getFromTracker("numSetupAttempts"),"1");
		assertEquals(getFromTracker("setupDuration"),"0 min, 0 sec");
	}
	
	@Test
	public void testError() {
		addStdoutRule("verification", "3");
		try {
			provisioner.setup(environment,true);
			fail();
		} catch (SetupException e) {
			assertTrue(getFromTracker("errorMessage").toString().contains("Error while running the vm_creation phase"));
			assertEquals(getFromTracker("statusCode"), String.valueOf(StatusCode.SystemError.code));
			assertEquals(getFromTracker("setupResult"),State.ERROR.name());
			assertEquals(getFromTracker("numSetupAttempts"),String.valueOf(InstanceCreator.NUMBER_OF_TRIES));
		} 
	}
	
	@Test
	public void testInit() throws SetupException, ResourceBusyException {
		assertEquals(MongoTracker.getLastDocument().get("setupResult"),
				State.INITIALIZED.name());
	}
	
	@Test
	public void testInitError() throws SetupException, ResourceBusyException {
		addStdoutRule("vagrant -v", VAGRANT_ERROR_MESSAGE);
		try {
		    provisioner.init(new Model(new File("models/a-b-c-d.json"),2));
			fail();
		} catch (VagrantException e) {
			assertEquals(MongoTracker.getLastDocument().get("setupResult"),State.ERROR.name());
			assertEquals(MongoTracker.getLastDocument().get("errorMessage"),"Vagrant is not installed properly: "+VAGRANT_ERROR_MESSAGE);
		} 
	}
	
	@Test
	public void testPut() {		
		environment.put("a",1);
		assertEquals(getFromTracker("a"),"1");
		assertTrue(environment.getData().containsField("_run_id"));
	}
	
	@Test
	public void testDumpFieldsIncludesOnlyPublicFields() throws IllegalArgumentException, IllegalAccessException {
		environment.dumpFields();
		for (Field field : environment.getClass().getFields()) {
			if (field.get(environment)!=null) {
				assertTrue(environment.getData().containsField(field.getName()));
			}
		}
	}

	
	@Test
	public void updateTwice() throws InterruptedException {
		environment.updateData();
		assertEquals(getFromTracker("modelName"),"utest");
		environment.updateData();
		assertEquals(getFromTracker("modelName"),"utest");
	}
	
	/** 
	 * return the value for a given key from the tracker data object
	 * @param key key name
	 * @return String value
	 */
	private String getFromTracker(String key) {
		return environment.getData().get(key).toString();
	}
	
	
}
