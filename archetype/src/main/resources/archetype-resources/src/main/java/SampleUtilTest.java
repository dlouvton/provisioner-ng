package com.dlouvton.itest.utils;

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
public class SampleUtilTest {
	/**
	 * <brief, under 255 char, description> 
	 * <long description>
	 * @expectedResults
	 */
	@Test
	public void sampleUtilUnitTest()  {
		assertThat("Check the output of hello()", SampleUtil.hello(), containsString("Hello"));
	}	
}
