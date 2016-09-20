package com.dlouvton.badger.provisioner.status;
/***
 * Status codes
 * Every environment (or the global Status) can have fields that return a status code, i.e. Success.
 * Every StatusCode has code that is an integer
 */
public enum StatusCode {
	Success(0), SystemError(1), ClientError(2), ModelInitializationError(3), Interrupted(4);
	public int code;
    private StatusCode(int code) {
            this.code = code;
    }
}

