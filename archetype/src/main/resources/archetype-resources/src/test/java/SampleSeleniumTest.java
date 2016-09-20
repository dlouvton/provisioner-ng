package com.dlouvton.itest;

import java.io.IOException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.dlouvton.badger.itest.common.SeleniumTest;

/**
 * Example Selenium test against Git web page title.
 */
public class SampleSeleniumTest extends SeleniumTest {

	public static final String ORG_GIT_URL = "https://github.com";
	WebDriver driver;

	@BeforeMethod
	public void before() throws Exception {
		try {
			driver = getWebDriver(ORG_GIT_URL);
		} catch (WebDriverException e) {
			LOG.severe("Before method failed in SeleniumSampleTest using URL " + ORG_GIT_URL + ": "
					+ e.getMessage());
			// Intentionally swallow; if left alone, will cancel ALL remaining
			// test cases, even the ones outside of this class.
		}
	}

	@AfterMethod
	public void after() {
		try {
			if (driver != null) {
				driver.close();
			}
		} catch (WebDriverException e) {
			LOG.severe("After method failed in SeleniumSampleTest: "
					+ e.getMessage());
			// Intentionally swallow; if left alone, will cancel ALL remaining
			// test cases, even the ones outside of this class.
		}
	}

	/**
	 * Verify dlouvton github page title
	 * Used as illustration of a Selenium test and basic check of using SeleniumTest class by extension.
	 * @priority High
	 * @expectedResults GitHub is page title
	 * @userStory W-2027865 
	 * @hierarchy
	 */
	@Test(groups = "HighPriority")
	public void sampleSeleniumTest() throws IOException, InterruptedException {
		LOG.info("Running org_github_title selenium test");

		if (driver != null) {
			assertThat( "GitHub URL " + ORG_GIT_URL + " page title",
						driver.getTitle(), containsString("GitHub"));	                   
		}
		else {
			Assert.fail("No Selenium web driver to verify github URL with");
		}
	}
	
}

