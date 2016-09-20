package com.dlouvton.badger.itest.common;

import org.testng.SkipException;

public class BadgerSkipException extends SkipException {

	public BadgerSkipException(String skipMessage) {
		super(skipMessage);
		reduceStackTrace();
	}

	private static final long serialVersionUID = 1L;
}
