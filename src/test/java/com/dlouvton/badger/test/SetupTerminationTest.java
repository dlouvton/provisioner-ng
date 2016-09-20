package com.dlouvton.badger.test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.dlouvton.badger.api.server.ResourceBusyException;
import com.dlouvton.badger.provisioner.Environment;
import com.dlouvton.badger.provisioner.SetupException;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.provisioner.model.Model;
import com.dlouvton.badger.provisioner.phases.InstanceCreator;
import com.dlouvton.badger.provisioner.phases.RetryException;
import com.dlouvton.badger.provisioner.status.State;
import com.dlouvton.badger.util.CmdRule;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

import static com.dlouvton.badger.util.FakeCommandLine.*;

public class SetupTerminationTest {

	VagrantProvisioner provisioner;
	private static final String message = "Setup was expected to fail and throw a VagrantException";
	Environment environment;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() throws SetupException, ResourceBusyException {
		Utils.removeAllEntriesFromMap(VagrantProvisioner.getInstance()
				.getEnvironments());
		FakeCommandLine.clearRules();
		provisioner = VagrantProvisioner.getInstance();
		provisioner.setProvisionerService(new FakeCommandLine());
		addStdoutRule("vagrant -v", "Vagrant 1.6.3");
		VagrantProvisioner.performSetup(true);
		VagrantProvisioner.performTeardown(true);
		environment = provisioner.init(new Model(
				new File("models/a-b-c-d.json"), 1));

	}

	@Test
	public void testDestroyOnExceptionDestroyedOK() {
		// since one comp is static, the up will fail (expecting 4),
		// but destroy will be ok
		addStdoutRule("verification", "3");
		thrown.expect(SetupException.class);
		thrown.expectMessage("Setup failed");
		provisioner.setup(environment, true);
	}

	@Test
	public void testDontDestroyOnError() {
		// when you pass false to setup the model destruction should not happen
		addStdoutRule("verification", "3");
		CmdRule rule = addExitcodeRule("vagrant destroy d__1 c__1 b__1 -f",
				"99");
		try {
			provisioner.setup(environment, false);
			fail(message);
		} catch (SetupException ve) {
			assertTrue(!rule.visited);
		}
	}

	@Test
	public void testDestroySingleComponent() {
		// Here we verify that the system recovers from bad instance creation.
		// On first try, parallel setup fails. On the second attempt one
		// component fails to start
		// Then we verify that the bad component ('d') is being destroyed.
		// On the last retry all components start up
		addStdoutRule("verification", "3");
		addStdoutRule("verification 2", "4");
		addStdoutRule("verification 3", "4");
		addExitcodeRule("attempt 2", "99");
		CmdRule rule = addExitcodeRule("vagrant destroy d__1 -f", "0",
				"vagrant up d__1");
		provisioner.setup(environment, true);
		assertTrue(rule.visited);

	}

	@Test
	public void testDestroyOnExceptionFailedToDestroyed() {
		// Setup will fail. Destroy will fail as well since we're expecting 3
		// after the destruction
		addStdoutRule("verification", "2");
		thrown.expect(VagrantException.class);
		thrown.expectMessage("Error while destroying the model");
		provisioner.setup(environment, true);
	}

	@Test
	public void testRetryUP() {
		try {
			addStdoutRule("verification", "3");
			provisioner.up();
			fail(message);
		} catch (VagrantException e) {
			assertTrue(e.getCause() instanceof RetryException);
			assertTrue(e.getCause().getMessage()
					.contains(InstanceCreator.NUMBER_OF_TRIES+" attempts to try failed"));
		}
	}

	@Test
	public void testRetryFailed() {
		try {
			addStdoutRule("verification", "3");
			addExitcodeRule("attempt 2", "99");
			provisioner.up();
			fail(message);
		} catch (VagrantException e) {
			assertTrue(e.getCause() instanceof RetryException);
			assertTrue(e.getCause().getMessage()
					.contains(InstanceCreator.NUMBER_OF_TRIES+" attempts to try failed"));
		}
	}

	@Test
	public void testDontDestroyOnFirstTry() {
		// here we verify the d__1 up happened after the parallel up was
		// failing, without destroying components first
		addStdoutRule("verification 2", "4");
		addExitcodeRule("attempt 1", "99");
		CmdRule rule = addStdoutRule("d__1 up attempt 2", "OK",
				"vagrant up d__1 b__1 a__1");
		provisioner.up();
		assertTrue(rule.visited);
	}

	@Test
	public void testAfterFirstTryDestroyIncludingNonReused() {
		// here we verify we destroy -f (force) on second try 
		addStdoutRule("verification", "3");
		addStdoutRule("verification 3", "4");
		addExitcodeRule("attempt 1", "99");
		addExitcodeRule("attempt 2", "99");
		CmdRule rule = addExitcodeRule("vagrant destroy d__1 -f", "0",
				"vagrant up d__1");
		provisioner.up();
		assertTrue(rule.visited);
	}

	
	@Test
	public void testComponentStatusLoggingBetweenRetries() {
		addStdoutRule("verification 2", "4");
		addExitcodeRule("attempt 1", "99");
		CmdRule rule = addStdoutRule("d__1 up attempt 2", "OK",
				"vagrant up d__1 b__1 a__1");
		provisioner.up();
		assertTrue(rule.visited);
	}
	
	@Test (expected = InterruptedException.class)
	public void testInterrupt() {
		addStdoutRule("verification", "4");
		CmdRule rule = addExitcodeRule("attempt 1", "0");
		rule.injectExceptionOnMatch(new InterruptedException());
		provisioner.setup(environment, true);
		assertEquals(environment.environmentStatus, State.INTERRUPTED.name());
	}
}
