package com.dlouvton.badger.test;

import static junit.framework.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.util.CmdRule;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

import static com.dlouvton.badger.util.FakeCommandLine.*;

public class ProvisionTest {

	VagrantProvisioner provisioner;
	Environment environment;

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
	}

	@Test
	public void testVagrantNotInstalled()  {
		try {
			addStdoutRule("vagrant -v", "vagrant is not installed");
		} catch (VagrantException e) {
			assertTrue(e.getMessage().contains("failed to initialize vagrant"));
		}
	}

	@Test
	public void testInit() throws SetupException, ResourceBusyException  {
		assertTrue(provisioner.getEnvironment(1).isStateEquals(State.INITIALIZED));
	}

	@Test
	public void testBring4ComponentsUp()  {
		// this will verify that 4 components are up after doing the last
		// ssh-config
		addStdoutRule("verification", "4", "vagrant ssh-config c__1");
		provisioner.up();
		assertNotSame(State.ERROR, provisioner.getEnvironment(1).environmentStatus);
	}

	@Test
	public void testParallelThenSerial()  {
		// this will verify that 3 components are up destroying model on error (one left - static)
		addStdoutRule("verification: model is destroyed", "3");
		addStdoutRule("verification 2", "4");
		//check that first attempt is in parallel
		CmdRule rule1 = addExitcodeRule("vagrant up d__1 b__1 a__1.*attempt 1", "99");
		//check that second attempt is serial
		CmdRule rule2 = addExitcodeRule("vagrant up d__1.*attempt 2", "0");
		CmdRule rule3 = addExitcodeRule("vagrant up b__1.*attempt 2", "0");
		provisioner.up();
		assertTrue(rule1.visited);
		assertTrue(rule2.visited);
		assertTrue(rule3.visited);
	}
	
	
	@Test
	public void testInstallOK()  {
		// validate that no exception is thrown
		addStdoutRule("verification", "4");
		addExitcodeRule("--provision-with puppet", "0");
		addStdoutRule("--provision-with puppet", "Fake manifest completed ok");
		provisioner.up();
		provisioner.install();
	}

	@Test(expected = SetupException.class)
	public void testBadInstall()  {
		addStdoutRule("verification", "4");
		addExitcodeRule(".*--provision-with puppet", "99");
		provisioner.setup(environment, false);
	}

	@Test
	public void testConfigure()  {
		// check that b is being configured before a, and no exception is thrown
		CmdRule rule = addExitcodeRule("a__1 --provision-with shell", "0",
				"b__1 --provision-with shell");
		provisioner.configure();
		assertTrue(rule.visited);
	}

	@Test
	public void testDestroy()  {
		// check that dynamic components are being destroyed
		addStdoutRule("verification", "4");
		addStdoutRule("verification: model is destroyed", "3");
		CmdRule rule = addStdoutRule("vagrant destroy d__1 c__1 b__1", "0");
		provisioner.setup(environment, true);
		provisioner.destroy(environment);
		assertTrue(rule.visited);
	}
	
	@Test
	public void testDefaultAndNonDefaultProvider()  {
		addStdoutRule("verification", "4");
		CmdRule rule1 = addExitcodeRule("vagrant ssh-config d__1", "0",
				"vagrant up d__1 b__1 a__1 --provider=dummy");
		CmdRule rule2 = addExitcodeRule("vagrant up c__1 --provider=dummyManaged",
				"0", "vagrant ssh-config a__1");
		provisioner.up();
		assertTrue(rule1.visited);
		assertTrue(rule2.visited);

	}
	
	@Test
	public void testBadConfigForOneComponent()  {
		addExitcodeRule("a__1 --provision-with shell", "99");
		try {
			provisioner.configure();
		} catch (VagrantException e) {
			assertTrue(e
					.getMessage()
					.contains(
							"component a__1 is not configured properly; configuration script returned 99"));
		}
	}
}
