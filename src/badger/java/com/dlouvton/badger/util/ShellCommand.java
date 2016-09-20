package com.dlouvton.badger.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ShellCommand implements Runnable {

	private File workingDirectory;
	protected int exitValue;
	protected StringBuffer stdout;
	protected StringBuffer stderr;
	public String executable;
	private List<String> options = new ArrayList<String>();
	public List<String> args = new ArrayList<String>();
	private List<String> etcs = new ArrayList<String>();
	private boolean isExecuting = false;
	private Process process = null;
	private final String outputPrefix;

	protected static final String LS = System.getProperty("line.separator");
	protected static final Logger LOG = CustomLoggerFactory
			.getLogger(ShellCommand.class);

	public ShellCommand(File workDirectory) {
		this(workDirectory, "");
	}

	public ShellCommand(File workDirectory, String outputPrefix) {
		setWorkingDirectory(workDirectory);
		this.stdout = new StringBuffer();
		this.stderr = new StringBuffer();
		this.outputPrefix = outputPrefix;
	}

	public int getExitValue() {
		return exitValue;
	}

	public String getStdout() {
		return this.stdout.toString();
	}

	public String getStderr() {
		return this.stderr.toString();
	}

	public File getWorkingDirectory() {
		return this.workingDirectory;
	}

	public void setWorkingDirectory(File path) {
		if (!path.exists() || !path.isDirectory()) {
			throw new RuntimeException("The working directory " + path
					+ " does not exist");
		}

		this.workingDirectory = path;
	}

	public String getCmdLine() {
		StringBuffer sb = new StringBuffer(executable);
		sb.append(" ");
		sb.append(this.joinList(args));
		sb.append(" ");
		sb.append(this.joinList(options));
		sb.append(" ");
		sb.append(this.joinList(etcs));

		return sb.toString().trim().replaceAll("\\s+", " ");
	}

	private String joinList(List<String> list) {
		String str = "";

		for (String s : list) {
			str += (str.equals("") ? "" : " ");
			str += s;
		}

		return str;
	}

	public ShellCommand appendOption(String option) {
		options.add(option);

		return this;
	}

	public ShellCommand appendArg(String arg) {
		args.add(arg);

		return this;
	}

	public ShellCommand appendEtc(String etc) {
		etcs.add(etc);

		return this;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public ShellCommand pipeline(String line) {
		return appendEtc("|").appendEtc(line);
	}
	
	public ShellCommand comment(String comment) {
		return appendEtc("`# "+comment+"`");
	}

	public List<String> getStdoutLines() {
		return Arrays.asList(getStdout().split(LS));
	}

	public void execute() {
		this.run();
	}

	public void executeAsync() {
		// Check if it's currently executing
		if (this.isExecuting) {
			throw new RuntimeException(
					"ShellCommand is executing and cannot be executed again.");
		}

		// Immediately mark as executing so it can't be executed again while
		// it's still underway
		this.isExecuting = true;

		// Kick the process
		try {
			String cmdline = getCmdLine();
			LOG.fine("Running command: '" + cmdline + "' ... ");
			String[] str = { "/bin/sh", "-c", cmdline };
			this.process = Runtime.getRuntime().exec(str, null,
					workingDirectory);

			// Capture STDERR
			StreamGobbler errorGobbler = new StreamGobbler(
					this.process.getErrorStream(),
					this.outputPrefix + "STDERR", stderr, true);
			errorGobbler.start();

			// Capture STDOUT
			StreamGobbler outputGobbler = new StreamGobbler(
					this.process.getInputStream(), this.outputPrefix, stdout,
					true);
			outputGobbler.start();
		} catch (Exception e) {
			throw new RuntimeException("Error in running command '"
					+ this.getCmdLine() + "'", e);
		}
	}

	public void waitForCompletion() {
		try {
			this.process.waitFor();
		} catch (Exception e) {
			throw new RuntimeException("Error upon completion of command '"
					+ this.getCmdLine() + "'", e);
		}

		exitValue = this.process.exitValue();
		LOG.fine("Exit code for process '" + this.getCmdLine() + "': "
				+ exitValue);
		this.isExecuting = false;
	}

	public void run() {
		this.executeAsync();
		this.waitForCompletion();
	}
	
	public void clear() {
		stderr.setLength(0);
		stdout.setLength(0);
		args.clear();
		options.clear();
		etcs.clear();
	}
}
