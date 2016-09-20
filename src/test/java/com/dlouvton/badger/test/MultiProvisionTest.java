package com.dlouvton.badger.test;


import java.io.File;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.api.server.BadgerServerException;
import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

import static com.dlouvton.badger.util.FakeCommandLine.*;
import static junit.framework.Assert.assertEquals;

public class MultiProvisionTest {

	VagrantProvisioner provisioner;
	Environment environment1;
	Environment environment2;

	@Before
	public void setup() throws SetupException, ResourceBusyException  {
		Utils.removeAllEntriesFromMap(VagrantProvisioner.getInstance().getEnvironments());
		FakeCommandLine.clearRules();
		provisioner = VagrantProvisioner.getInstance();
		provisioner.setProvisionerService(new FakeCommandLine());
		addStdoutRule("vagrant -v", "Vagrant 1.6.3");
		VagrantProvisioner.performSetup(true);
		VagrantProvisioner.performTeardown(true);		
		environment1 = provisioner.init(new Model(new File("models/a-b-c-d.json"),1));
		environment2 = provisioner.init(new Model(new File("models/a-b-c-d.json"),2));		
	}
	
	@After
	public void after() {
		provisioner.getEnvironments().remove(environment1);
		provisioner.getEnvironments().remove(environment2);
	}

	@Test(expected = BadgerServerException.class) 
    // we only allow 2 environments
	public void testThreeSetupsSerially() throws Throwable{
		addStdoutRule("verification", "4");
		addStdoutRule("verification 1", "3");
		provisioner.setup(environment1, false);
		provisioner.setup(environment2, false);
		provisioner.setup(provisioner.init(new Model(new File("models/a-b-c-d.json"),3)), true);
	}
	
	@Test 
	public void testBasic() {
		Assert.assertEquals(2, provisioner.getEnvironments().size());
		Assert.assertEquals(State.INITIALIZED.name(), provisioner.getEnvironment(1).environmentStatus);		
	}
	
	@Test 
	public void testComponentRename() {
		Assert.assertEquals("[d__1, c__1, b__1, a__1]", provisioner.getEnvironment(1).getModel().getComponentsStr());
		Assert.assertEquals("[d__2, c__2, b__2, a__2]", provisioner.getEnvironment(2).getModel().getComponentsStr());
	}
	
	@Test 
	public void testTwoSetupsSerially() {
		addStdoutRule("verification", "4");
		addStdoutRule("verification 1", "2");
		provisioner.setup(environment1, false);
		provisioner.setup(environment2, false);
		assertEquals(State.READY.name(), provisioner.getEnvironment(1).environmentStatus);
		assertEquals(State.READY.name(), provisioner.getEnvironment(2).environmentStatus);
		
	}

	@Test (expected = ResourceBusyException.class)
	public void testBusy() throws SetupException, ResourceBusyException {
		addStdoutRule("verification", "4");
		environment1.environmentStatus= State.BUSY.name();		
		environment1 = provisioner.init(new Model(new File("models/a-b-c-d.json"),1));
		provisioner.setProvisionerService(new FakeCommandLine());
		provisioner.setup(environment1, false);		
	}
	
}
