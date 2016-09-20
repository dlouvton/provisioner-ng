package com.dlouvton.badger.provisioner.phases;

public class RetryException extends Exception {

	private static final long serialVersionUID = 1L;

	public RetryException() {
		super();
	}

	public RetryException(String message) {
		super(message);
	}

	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryException(Throwable cause) {
		super(cause);
	}
}
