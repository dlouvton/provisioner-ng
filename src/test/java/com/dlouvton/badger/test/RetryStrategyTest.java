package com.dlouvton.badger.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import com.dlouvton.badger.provisioner.phases.RetryException;
import com.dlouvton.badger.provisioner.phases.RetryStrategy;

public class RetryStrategyTest {

	RetryStrategy retryStrategy;

	@Before
	public void setup() {
		retryStrategy = new RetryStrategy(2, 1);
	}

	@Test
	public void testProgressivelyLongerWaitTimeBetweenRetries()
			throws RetryException {
		assertEquals(retryStrategy.getTimeToWait(), 1);
		retryStrategy.errorOccured();
		assertEquals(retryStrategy.getTimeToWait(), 2);	
	}
	
	@Test (expected=RetryException.class)
	public void testExhustRetries()
			throws RetryException {
		while (retryStrategy.shouldRetry()) {
			retryStrategy.errorOccured();
		}
	}
	
}