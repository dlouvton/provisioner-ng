package com.dlouvton.badger.provisioner;

public class SetupException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SetupException(String message) {
		super(message);
	}

	public SetupException(String message, Throwable cause) {
		super(message, cause);
		cause.printStackTrace();
	}
}