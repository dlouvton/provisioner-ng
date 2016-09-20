package com.dlouvton.badger.api.server;

public class ResourceBusyException extends Exception {

	private static final long serialVersionUID = 1L;

	public ResourceBusyException(String message) {
		super(message);
	}

	public ResourceBusyException(String message, Throwable cause) {
		super(message, cause);
		cause.printStackTrace();
	}
}