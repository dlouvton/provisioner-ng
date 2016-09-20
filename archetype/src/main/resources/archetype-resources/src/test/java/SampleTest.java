package com.dlouvton.itest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.dlouvton.badger.itest.common.BaseTest;

/**
 * Describe your class -- this example just logs entry into methods.
 */
public class SampleTest extends BaseTest {


	@BeforeMethod
	public void before() {
		LOG.info("Execute before method ...");
	}

	@AfterMethod
	public void after() {
		LOG.info("Execute after method ...");
	}

	/**
	 * <brief, under 255 char, description> 
	 * <long description>
	 * @expectedResults
	 * @userStory W-#######
	 */
	@Test(groups = "LowPriority")   // @Test(timeOut = 190000, groups = { "HighPriority" }) or MediumPriority
    // @Requires({"component1","compoment2"})
	public void sampleTest()  {
		LOG.info("sampleTest is Starting");
		LOG.fine("sampleTest showing failure");
		assertThat("description", "not implemented result", containsString("actual"));
		LOG.info("sampleTest Complete");
	}	
}
