package com.dlouvton.badger.util;

public class CmdRule {

	public String type;
	public String match;
	public String output;
	public String lastcmdmatch;
	public boolean visited;
	public static final String STDOUT="stdout";
	public static final String STDERR="stderr";
	public static final String EXITCODE="exitcode";
	public Throwable thrownException;
	
	public CmdRule(String type, String match, String output, String lastcmdmatch) {
		super();
		this.type = type;
		this.match = ".*"+match+".*";
		this.output = output;
		this.lastcmdmatch = ".*"+lastcmdmatch+".*";
		this.visited=false;
	}
	
	/** 
	 * set the exception type to throw when the current rule is matched
	 * @param e Exception to throw
	 */
	public void injectExceptionOnMatch (Throwable e){
		thrownException = e;
	}
}
