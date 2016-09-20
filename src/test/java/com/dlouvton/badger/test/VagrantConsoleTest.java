package com.dlouvton.badger.test;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.provisioner.VagrantCommand;
import com.dlouvton.badger.provisioner.VagrantException;
import com.dlouvton.badger.provisioner.model.Component;

public class VagrantConsoleTest {

	VagrantCommand cmd;

	@Before
	public void setup() throws Exception {
		cmd = new VagrantCommand(new File("vagrant"));
	}

	@Test
	public void testCommandIsOK() {
		cmd.setService("up");
		cmd.debug();
		cmd.machineReadable();
		cmd.setTargetMachine("target");
		cmd.setProvisioner("shell");

		assertEquals(cmd.getCmdLine(),
				"vagrant up target --debug --machine-readable --provision-with shell");
	}

	@Test
	public void testCommandIsOKMultipleMachines() {
		List<Component> components = new ArrayList<Component>();
		Map<String,String> props = new HashMap<String,String>();
		components.add(new Component("alpha", props, null));
		components.add(new Component("bravo", props, null));
		components.add(new Component("charlie", props, null));

		cmd.setService("up");
		cmd.setTargetMachines(components);
		cmd.setProvisioner("shell");

		assertEquals(cmd.getCmdLine(),
				"vagrant up alpha bravo charlie --provision-with shell");
	}

	@Test(expected = VagrantException.class)
	public void testWhenServiceIsNotSet() {
		cmd.setTargetMachine("target");
		cmd.setProvisioner("shell");
		cmd.execute();
	}

	@Test(expected = VagrantException.class)
	public void test2ProvisionCallsNotAllowed() {
		cmd.setTargetMachine("target");
		cmd.setProvisioner("puppet");
		cmd.noProvision();
		cmd.execute();
	}

	@Test(expected = RuntimeException.class)
	public void testWorkdirNotExists() {
		cmd = new VagrantCommand(new File("notfound"));
	}
}
