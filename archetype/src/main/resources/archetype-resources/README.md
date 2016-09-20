${artifactId} Integration Tests
================================

###Remote Test Execution
Your tests can only communicate with the environment in aws, so your tests must be executed in aws. We provide a simple script called remote-run.sh (in the top level project directory) to take care of remote execution for you. After you have finished writing your tests, simply ./remote-run.sh and the script with copy your tests to aws and execute them. Outputs of your tests will be printed to stdout, the log will be copied to the logs/ directory, and any surefire reports generated will be copied into target/surefire-reports.

**NOTE:** The following aws Setup can be skipped if you opt to use the remote-run.sh script


###aws Setup
If you'd like to run the tests on aws, you may have to install a Test Runner by following these directions, and then ssh into the newly created instance. See https://github.com/dlouvton/badger-itest for example of running tests.

The easiest way to install a remote server is to install it from Jenkins (e.g. http://10.224.57.220/job/install-badger-server/), or running this:
Note: please edit stage/scripts/scripts.properties, with the correct gitUrl.

```
echo "gitUrl=<url-of-git-repo>" > stage/scripts/scripts.properties
#install server
chmod +x run.sh
./run.sh remote-install
#connect to sever
./run.sh ssh
```

#### Advanced installation and Troubleshooting:

You may use an advanced script, located at scripts/install_aws_instance.sh, to install a remote server if you run into issues. It takes a unique_name that is used to create a private key that is required to create instances on aws.  The unique_name consists of letters, numbers, or hyphens.  For example username-badger.  The script appends to /etc/ssh/ssh_config, ~/.bashrc, and ~/.profile files which should not be open while the scripts run.

**NOTE:** These scripts will append to your .bashrc file. If you do not want them to, you can either set the variable BADGER_UPDATE_BASHRC=0 in the environment, or add "export BADGER_UPDATE_BASHRC=0" to your .bashrc

The script may prompt for sudo password.

If you have Ruby installed locally:
```
cd ${artifactId}/scripts
./install_aws_testrunner.sh <uniqueName>
source ~/.bashrc
cd
#assuming that the folder ${artifactId} is located under ~/git
scpv git/${artifactId} git/${artifactId}
sshv
cd ~/git/${artifactId}   #run this on the remote instance
./run.sh
```


To install Ruby and setup aws:
```
cd ${artifactId}/scripts
./install_aws_testrunner.sh <uniqueName> CLEAN
source ~/.bashrc
cd
#assuming that the folder ${artifactId} is located under ~/git
scpv git/${artifactId} git/${artifactId}
sshv
cd ~/git/${artifactId}   #run this on the remote instance
./run.sh
```

To create a separate Ruby-Box to provision the aws instance from:
```
cd ${artifactId}/scripts
./install_aws_testrunner.sh <uniqueName> VM
cd ~/ruby-box; vagrant ssh
source ~/.bashrc
cd
#assuming that the folder ${artifactId} is located under ~/git
scpv git/${artifactId} git/${artifactId}
sshv
cd ~/git/${artifactId}   #run this on the remote instance
./run.sh
```

####Options
* Default: Creates a aws instance that can be ssh'd into and controlled from the local machine (NOTE: Only works if Ruby is already installed on the local machine)
* VM: Creates a Ruby-Box on the local machine. The created aws-Instance can only be ssh'd into and controlled from the Ruby-Box.
* CLEAN: Destroys any currently running aws Instance, attempts to install Ruby on the local machine, and then sets up a aws instance which can be ssh'd into and controlled from the local machine.


###Running Tests
edit your user.properties, to reflect the model that you'd like to use, and other configuration values, for example:
````
	log-level=FINE
	model-path=src/resources/${artifactId}.json
	post-to-mongo=true
	private-key="dlouvton-key-pair"
	private-key-path="./dlouvton-key-pair"
```
then you can run the tests using ./run.sh
These tests are TestNG tests that use the Badger framework.
