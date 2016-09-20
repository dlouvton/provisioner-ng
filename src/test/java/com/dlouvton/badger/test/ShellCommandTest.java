package com.dlouvton.badger.test;

import static org.junit.Assert.*;
import static org.testng.Assert.assertEquals;

import java.io.File;
import org.junit.Before;
import org.junit.Test;

import com.dlouvton.badger.util.ShellCommand;

public class ShellCommandTest {

	ShellCommand cmd;

	@Before
	public void setup()  {
		cmd = new ShellCommand(new File("vagrant"));
		cmd.setExecutable("pwd");
	}
	
	/* test async shell command 
	 * @result execute async will provide the same result as a non-async execcute
	 */
	@Test
	public void testAsync() throws InterruptedException {
		cmd.executeAsync();
		assertEquals(cmd.getCmdLine(), "pwd");
	}

	/* clear the args 
	 * @result command line contains only the executable
	 */
	@Test
	public void testClear()  {
		cmd.appendArg("arg1");
		cmd.appendEtc("etc1");
		cmd.clear();
		assertEquals(cmd.getCmdLine(), "pwd");
	}
	
	/* execute non-async 
	 * @result exit value is properly generated
	 */
	@Test
	public void testNonAsync() {
		cmd.execute();
		assertEquals(cmd.getCmdLine(), "pwd");
		assertEquals(cmd.getExitValue(),0);
	}
	

}
