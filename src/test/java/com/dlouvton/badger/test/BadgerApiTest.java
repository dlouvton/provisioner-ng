package com.dlouvton.badger.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static com.dlouvton.badger.util.FakeCommandLine.addExitcodeRule;
import static com.dlouvton.badger.util.FakeCommandLine.addStdoutRule;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.*;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.dlouvton.badger.api.server.BadgerServer;
import com.dlouvton.badger.provisioner.VagrantProvisioner;
import com.dlouvton.badger.util.CmdRule;
import com.dlouvton.badger.util.FakeCommandLine;
import com.dlouvton.badger.util.Utils;

public class BadgerApiTest {

	VagrantProvisioner provisioner;
	//Need a non-default port 8081 so the tests can run on a Jenkins box
	public static final int SERVER_PORT=8081;

	@Before
	public void setup() throws Exception {
		FakeCommandLine.clearRules();
		provisioner = VagrantProvisioner.getInstance();
		Utils.removeAllEntriesFromMap(provisioner.getEnvironments());
		provisioner.setProvisionerService(new FakeCommandLine());
		addStdoutRule("vagrant -v", "Vagrant 1.6.3");
		VagrantProvisioner.performSetup(true);
		VagrantProvisioner.performTeardown(true);		
		BadgerServer.start(SERVER_PORT);
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = SERVER_PORT;
		RestAssured.basePath = "/v1";
		addStdoutRule("verification", "4");		
	}

	@Test
	public void testStatusLog() throws IOException {
		final String LOG_MESSAGE="log_message";
		Path outLog = Paths.get("out.log");
		OutputStream output = Files.newOutputStream(outLog, CREATE, TRUNCATE_EXISTING);
		output.write(LOG_MESSAGE.getBytes());
		
		expect().
	    statusCode(200).
	    body(containsString(LOG_MESSAGE)).
	    when().
	    get("/main/about?action=log");
		
		output.close();
		Files.delete(outLog);
	}
	
	@Test
	public void testStatus() throws Exception {
		expect().
	    statusCode(200).
	    body(
				"systemStatus", equalTo("READY")).
	    when().
	    get("/main");
		
	}

	@Test
	public void testVersion() throws Exception {
		expect().
		statusCode(200).
		body(containsString("Version")).
		when().
		get("/main/about?action=version");

	}


	/* Must pass envID when starting a model
	 * @result BadRequestException
	 */
	@Test
	public void testNoEnvID() {
		expect().
	    statusCode(400).
	    when().
	    get("/model/does-not-exist.json?action=start");
	}
	
	@Test
	public void testBadModel() {
		expect().
	    statusCode(500).
	    when().
	    get("/model/does-not-exist.json?action=start&envID=1");
	}
	
	@Test
	public void testModelCreation() {
		expect().
	    statusCode(200).
	    body(
	    		"environmentId", equalTo("1"),
	    		"modelName", equalTo("utest"),
	    		"statusCode", equalTo("0")	    		).
	    when().
	    get("/model/a-b-c-d.json?action=start&envID=1");
	}
	
	/* Empty environment list
	 * @result 0 environments when no model was started
	 */
	@Test
	public void testEmptyEnvList() {
		expect().
	    statusCode(200).
   		body("numberOfEnvironments", equalTo("0")).    
	    when().
	    get("/environment");
	}
	
	@Test
	public void testEndToEnd()  {
		setupEnv(1);	
	    //verify environmentList (GET /environment)
		expect().
	    body("numberOfEnvironments", equalTo("1")).    		
	    when().	    
	    get("/environment");
		
		//verify environment (GET /environment/{id}
		expect().
	    statusCode(200).
	    body(
	    		"statusCode", equalTo("0"),
	    		"environmentStatus", equalTo("READY"),
	    		"setupDuration", equalTo("0 min, 0 sec")
	    		).    		
	    when().	    
	    get("/environment/1");	
		
		//verify environment destruction (DELETE /environment/{id})
	    addStdoutRule("verification", "3");

	    expect().
	    statusCode(200).
	    when().	  
		delete("/environment/1");
	    get("/environment/1");

	    expect().
	    body("numberOfEnvironments", equalTo("0")).    		
	    when().	    
	    get("/environment");
	}
	
	// force_destroy_all destroys all environments
	@Test
	public void testForceDestroyAll()  {
		setupEnv(1);
		get("/environment/1/component?action=force_destroy_all");
		Assert.assertTrue(VagrantProvisioner.getInstance().getEnvironments().isEmpty());
		Assert.assertNull(VagrantProvisioner.getInstance().getEnvironment(1));
	} 
	
	// force_destroy_model destroys the current model
	@Test
	public void testForceDestroyModel()  {
		setupEnv(1);
		setupEnv(2);
		get("/environment/1/component?action=force_destroy_model");
		Assert.assertNotNull(VagrantProvisioner.getInstance().getEnvironment(2));
		Assert.assertNull(VagrantProvisioner.getInstance().getEnvironment(1));
	} 
	
	@Test
	public void testMultipleEnvironments()  {
		setupEnv(1);
		setupEnv(2);
	    get("/environment/1");
	    expect().
	    statusCode(200).
	    body(
	    		"environmentStatus", equalTo("READY"),
	    		"user", containsString("api")
	    		).    		
	    when().	    
	    get("/environment/2");
	}
	
	@Test
	public void testRunningMoreEnvironmentsThanAllowed() {
		for (int i=1; i<=VagrantProvisioner.MAX_NUM_ENVIRONMENTS; i++){
			get("/model/a-b-c-d.json?action=start&envID="+i);		
		}
		expect().
	    statusCode(500).
	    when().	    
		get("/model/a-b-c-d.json?action=start&envID="+VagrantProvisioner.MAX_NUM_ENVIRONMENTS+1);
	    expect().
	    statusCode(400).
	    when().	    
		get("/environment/"+VagrantProvisioner.MAX_NUM_ENVIRONMENTS+1);
	}
	
	/* get env.properties file (as text) 
	 * @result env.properties is verified
	 */
	@Test 
	public void testGetPlainProperties() {
		setupEnv(1);	
		given().contentType(ContentType.TEXT).
		expect().
	    statusCode(200).
	    body(containsString("#Badger created file")).
	    when().    
		get("/environment/1/plain-properties");
	}
	
	/* get component status from vagrant
	 * @result correct vagrant status
	 */
	@Test 
	public void testGetComponent()  {
		setupEnv(1);
		expect().
		statusCode(200).
		body("entries.entry.key", equalTo("dummy")).
	    when().   
		get("/environment/1/component");
	}
	
	/* action that is not supported 
	 * @result response 400
	 */
	@Test 
	public void testBadAction()  {
		setupEnv(1);
		expect().
		statusCode(400).
	    when().   
		get("/environment/1/component?action=badAction");
	}
	
	/* run ssh script 
	 * @result result contains exitcode and stdout
	 */
	@Test 
	public void testRunScript()  {
		addStdoutRule("vagrant ssh a__1 -c", "OK");	
		setupEnv(1);
	    expect().
	    statusCode(200).
	    body(
	    		"exitCode", equalTo("0"),
	    		"stdout", containsString("OK")
	    		).
	    when().    
		get("/environment/1/script/example.sh?action=run&component=a");
	}

	/* list available suites~
	 */
	@Test
	public void testSuiteList() {
		expect().
				statusCode(200).
				body(
						"scripts", hasItem("testng-itest.xml")).
				when().
				get("/environment/1/suite");
	}


	/* list available scripts 
	 * @result currently just one sample script is listed
	 */
	@Test 
	public void testScriptList() {
	    expect().
	    statusCode(200).
	    body(
	    		"scripts", equalTo("example.sh"),
	    		"scriptsLocation", equalTo("vagrant/post-install-scripts/")).    		
	    when().    
		get("/environment/1/script");
	}
	
	/* run an ssh command 
	 * @result the proper vagrant command (vagrant ssh a -c "ls") is constructed, Result resource has exitCode 0
	 */
	@Test 
	public void testRunCommand() {
		CmdRule rule = addExitcodeRule("vagrant ssh a__1 -c \"ls\"", "0");
		setupEnv(1);		
	    expect().
	    statusCode(200).
	    body(
	    		"exitCode", equalTo("0")
	    		).
	    when().    
		get("/environment/1/script/command?command=ls&action=run&component=a");
		assertTrue(rule.visited);
	}
	
	/* file upload 
	 * @result 200 response
	 */
	@Test 
	public void testFileUpload() {
		given().
		queryParam("destination","host").
        multiPart(new File("src/resources/sample.xml")).
        expect().
	    statusCode(200).
        when().
        post("/file");
	}
	
	/* cannot reuse an environment 
	 * @result BadgerServerException
	 */
	@Test 
	public void testEnvironmentReuseIsNoAllowed()  {
		setupEnv(1);
		expect().
		statusCode(500).
		when().	    
		get("/model/a-b-c-d.json?action=start&envID=1");
	}
	
	/* can reuse an environment that has an error 
	 * We bring up the model with errors, verify that you can reuse it.
	 * before resuing it, verify that it's being destroyed
	 */
	@Test 
	public void testEnvironmentReuseIsAllowedOnError()  {
		// inject a configuration error
		addExitcodeRule("a__1 --provision-with shell", "99");
		setupEnv(1);
		expect().
		statusCode(200).
		// verify that status is ERROR
		body("environmentStatus", equalTo("ERROR")).    		
		when().	    
		get("/environment/1");	
		// clear the error, so model will run fine next time
		addExitcodeRule("a__1 --provision-with shell", "0"); 
		CmdRule rule = addExitcodeRule("verification: model is destroyed", "0");
		addStdoutRule("verification", "3");
		addStdoutRule("verification 1", "4");
		// verify that env 1 can be reused
		expect().
		statusCode(200).
		when().	    
		get("/model/a-b-c-d.json?action=start&envID=1");
		// verify that the model with the error was destroyed 
		assertTrue(rule.visited);
	}
	
	/* verify that it's ok to destroy a non existing environment
	 */
	@Test
	public void destroyingEmptyEnvironment()  {
		setupEnv(1);
		addStdoutRule("verification", "3");
		delete("/environment/1");
		// destroying second time
		expect().
		statusCode(200).
		when().	  
		delete("/environment/1");
		// destroying third time globally
		expect().
		statusCode(200).
		when().	    
		get("/environment/1/component?action=force_destroy_all");
	}
	/* can reuse an environment if it's being destroyed first
	 * @result BadgerServerException
	 */
	@Test 
	public void testEnvironmentReuse()  {
		// try when env is force destroyed
		setupEnv(1);
		get("/environment/1/component?action=force_destroy_all");
		expect().
		statusCode(200).
		when().	    
		get("/model/a-b-c-d.json?action=start&envID=1");
		
		// try again when env is destroyed (no force)
		setupEnv(1);
		addStdoutRule("verification", "3");
		delete("/environment/1");
		expect().
		statusCode(200).
		when().	    
		get("/model/a-b-c-d.json?action=start&envID=1");
	}
	
	//handy method to setup an environment
	private void setupEnv(int envID) {
		get("/model/a-b-c-d.json?action=start&envID="+envID);
		try {
			Thread.sleep(500); //it takes a few milliseconds to complete setup
		} catch (InterruptedException e) {
			// swallow this exception
		}
	}
	
	@After
	public void teardown() throws Exception {
		BadgerServer.stop();
	}
}
