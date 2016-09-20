package com.dlouvton.badger.provisioner.phases;

public class RetryStrategy {

	public static final int DEFAULT_NUMBER_OF_TRIES = 3;
	public static final int DEFAULT_WAIT_TIME_SEC = 60;

	private int numberOfTries; // total number of tries
	private int numberOfTriesLeft; // number left
	private int timeToWaitInSec; // wait interval

	public RetryStrategy() {
		this(DEFAULT_NUMBER_OF_TRIES, DEFAULT_WAIT_TIME_SEC);
	}

	public RetryStrategy(int numberOfRetries, int timeToWaitSec) {
		this.numberOfTries = numberOfRetries;
		numberOfTriesLeft = numberOfRetries;
		this.timeToWaitInSec = timeToWaitSec;
	}

	public boolean shouldRetry() {
		return numberOfTriesLeft > 0;
	}

	public int tryNum() {
		return  numberOfTries - numberOfTriesLeft + 1;
	}

	public void errorOccured() throws RetryException {
		numberOfTriesLeft--;
		if (!shouldRetry()) {
			throw new RetryException(numberOfTries
					+ " attempts to try failed");
		}
		waitUntilNextTry();
	}

	/* calculates time to wait between tries.
	 * @returns a progressively longer time to wait between retries
	 */
	public int getTimeToWait() {
		return timeToWaitInSec * tryNum();
	}

	private void waitUntilNextTry() {
		try {
			Thread.sleep(getTimeToWait()*1000);
		} catch (InterruptedException ignored) {
		}
	}
}
