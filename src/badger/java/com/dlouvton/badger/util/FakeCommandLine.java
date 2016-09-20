package com.dlouvton.badger.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dlouvton.badger.provisioner.VagrantCommand;
import com.dlouvton.badger.tracker.MongoTracker;

public class FakeCommandLine extends VagrantCommand {

	// we use LinkedHashMap to keep the order of insertion
	static private List<CmdRule> stdoutRules = new ArrayList<CmdRule>();
	static private List<CmdRule> stderrRules = new ArrayList<CmdRule>();
	static private List<CmdRule> exitcodeRules = new ArrayList<CmdRule>();
	static private String lastcmd;
	

	public static boolean stopAtFirstMatch = false;

	public FakeCommandLine() {
		super(new File("."));
		reused = true;
		MongoTracker.skipUpload();
	}

	public ShellCommand setService(String service) {
		setExecutable("vagrant");
		serviceIsSet = true;
		lastcmd = getCmdLine();
		clear();
		args.add(service);
		return this;
	}

	public static void clearRules() {
	    stdoutRules.clear();    
		stderrRules.clear();
		exitcodeRules.clear();
    }
	
	public static CmdRule addStdoutRule(String match, String out) {
		return addRule(stdoutRules, CmdRule.STDOUT, match, out, ".*");
	}

	public static CmdRule addStderrRule(String match, String out) {
		return addRule(stderrRules, CmdRule.STDERR, match, out, "");
	}

	public static CmdRule addExitcodeRule(String match, String out) {
		return addRule(exitcodeRules, CmdRule.EXITCODE, match, out, "");
	}
	
	public static CmdRule addStdoutRule(String match, String out, String lastcmdmatch) {
		return addRule(stdoutRules, CmdRule.STDOUT, match, out, lastcmdmatch);
	}

	public static CmdRule addStderrRule(String match, String out, String lastcmdmatch) {
		return addRule(stderrRules, CmdRule.STDERR, match, out, lastcmdmatch);
	}

	public static CmdRule addExitcodeRule(String match, String out, String lastcmdmatch) {
		return addRule(exitcodeRules, CmdRule.EXITCODE, match, out, lastcmdmatch);
	}

	public static CmdRule addRule (List<CmdRule> rules, String type, String match, String out, String lastcmdmatch) {
		CmdRule rule = new CmdRule(type, match, out, lastcmdmatch);
		rules.add(rule);
		return rule;
	}

	@Override
	public void execute() {
		stdout = new StringBuffer(applyRules(stdoutRules,
				"FAKE STDOUT"));
		stderr = new StringBuffer(applyRules(stderrRules,
				"FAKE STDERR"));
		exitValue = Integer.parseInt(applyRules(exitcodeRules, "0"));
		LOG.fine("FAKE cmd: '" + getCmdLine() + "',  exitcode: "
				+ getExitValue()+", stdout: "
				+ getStdout() + ", stderr: " + getStderr());
	}

	public void waitForCompletion() {
		LOG.fine("FAKED waiting for completion of process");
	}

	public void executeAsync() {
		LOG.fine("FAKED sync execution");
		execute();
	}

	public String applyRules(List<CmdRule> rules, String defaultResponse)  {
		String response = defaultResponse;
		String cmdLine = getCmdLine();
		for (CmdRule rule : rules) {
			if (cmdLine.matches(rule.match)
					&& lastcmd.matches(rule.lastcmdmatch)) {
				response = rule.output;
				rule.visited = true;
				if (rule.thrownException != null) {
					Utils.<RuntimeException>throwException(rule.thrownException);
				}
				if (stopAtFirstMatch)
					break;
			}
		}
		return response;
	}

	public void stopAtFirstMatch() {
		stopAtFirstMatch = true;
	}
}