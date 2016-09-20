package com.dlouvton.badger.provisioner;

public class VagrantException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VagrantException() {
		super();
	}

	public VagrantException(String message) {
		super(message);
	}

	public VagrantException(String message, Throwable cause) {
		super(message, cause);
		cause.printStackTrace();
	}

	public VagrantException(Throwable cause) {
		super(cause);
		cause.printStackTrace();
	}
}