package com.dlouvton.badger.test;


import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.dlouvton.badger.itest.common.SeleniumTest;

public class TestNGSeleniumTest extends SeleniumTest {

		
	@Test
	public void testGetWebDriverWithUrl() throws ClientProtocolException, IOException {
		Assert.assertTrue(getWebDriver("https://github.com").getTitle().contains("GitHub"));
	}
	
	@Test (enabled = false)
	public void testGetWebDriverNoUrl() throws ClientProtocolException, IOException {
		WebDriver wd = getWebDriver();
		wd.get("https://github.com");
		Assert.assertTrue(wd.getTitle().contains("GitHub"));
	}
}
