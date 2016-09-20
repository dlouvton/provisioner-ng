package com.dlouvton.badger.test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.dlouvton.badger.itest.common.BadgerSkipException;
import com.dlouvton.badger.itest.common.BaseTest;

public class TestNGRunnerTest extends BaseTest {

	ITestContext context;

	@BeforeMethod
	public void before() throws Exception {
		context = mock(ITestContext.class);
	}

	/**
	 * Skip a test
	 * @result a skip exception will be thrown when a test calls get() with a key that does not exist
	 */
	@Test(expectedExceptions = BadgerSkipException.class)
	public void testThrowSkipException() {
		when(context.getAttribute(anyString())).thenReturn(null);
		get("key-that-does-not-exist");
	}

	/**
	 * Keys from properties are being copied to the test context
	 * @result the context contains the model-path that is being configured on user.properties
	 */
	@Test
	public void testModelKeysAreCopiedToContext() {
		when(context.getAttribute(anyString())).thenReturn(null);
		Assert.assertEquals(get("model-path"), "models/a-b-c-d.json");
	}
	
	/**
	 * test that additional data can be added to the database 
	 * @result a map containing by default model path and provision time
	 */
	@Test
	public void testAddAdditionalData() {
		Assert.assertEquals(additionalSuiteData().get("_model_path"), "models/a-b-c-d.json");
		Assert.assertEquals(additionalSuiteData().get("_provision_time"), "0 sec");
	}

	/**
	 * test that httpGet can fetch a url
	 * Using github.com instead of github.com since it's more stable 
	 * @throws IOException 
	 */
	@Test (enabled = false)
	public void testHttpGet() throws ClientProtocolException, IOException {
		Assert.assertTrue(httpGet("https://github.com").contains("GitHub"));
	}
}
