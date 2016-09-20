package com.dlouvton.badger.provisioner.status;

import javax.xml.bind.annotation.XmlRootElement;

import com.dlouvton.badger.provisioner.Environment;
/***
 * Status of the Badger program (global status)
 * this is a POJO that can easily be mapped as a response
 */
@XmlRootElement
public class Status {
	public State systemStatus = State.READY;
	public String[] modelNames;
	public Environment[] environments;
	public String errorMessage;

	public Status() {

	}

	
	public void error(Throwable e, StatusCode statusCode) {
		systemStatus = State.ERROR;
		errorMessage = e.getMessage()+"; Cause: "+e.getCause().getMessage();
	}

	
	public static Status notInitialized() {
		return new Status();
	}

}
