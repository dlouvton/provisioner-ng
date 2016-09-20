Badger
======

Badger provides the framework to deploy environments and run integration tests against multi-node software on AWS and other cloud providers.

###Develop Badger
To contribute to Badger, you may clone this project and import the maven project into Eclipse and install the Eclipse TestNG plugin. Once the project is imported, you can run tests as TestNG tests or execute the goal "mvn test".
Note that if you use your Eclipse setup, you will not be able to create instances on aws or access instances, but it might be useful for some tasks.

Pre-requisites: make sure you have maven installed.

```
git clone https://github.com/dlouvton/badger.git
cd badger
mvn install
```
To import the project to Eclipse, go to File / Import / Maven/ Existing Maven Project, and browse to the Badger folder.
To add the TestNG plugin to Eclipse:
Go to Help / Software updates / Find and Install, and enter http://beust.com/eclipse. Once the plugin is added restart Eclipse.

###AWS Setup
If you'd like to run the tests on aws, you may have to install a Test Runner by following these directions, and then ssh into the newly created instance.


####Install Badger as an API Server on aws
If you'd like to install a Badger server to accept provisioning requests, without running TestNG tests, you may install the TestRunner on aws with the SERVER option.
```
cd scripts
./install_aws_testrunner.sh <uniqueName> SERVER
```
After the script completes successfully, follow the instructions on the screen.

If you did not install the Test Runner as an API server, you may still start the server by running:
```
sshv
cd <my-project-name>
./run.sh start-server
```

######Options
* Default: Creates an aws instance that can be ssh'd into and controlled from the local machine (NOTE: Only works if Ruby is already installed on the local machine)
* VM: Creates a Ruby-Box on the local machine. The created aws-Instance can only be ssh'd into and controlled from the Ruby-Box.
* CLEAN: Destroys any currently running aws Instance, attempts to install Ruby on the local machine, and then sets up a aws instance which can be ssh'd into and controlled from the local machine.


Go to the VM that just got created in the aws
```
source ~/.bashrc
sshv
```

###Developing and Running Tests
In order to develop an integration test in the Badger framework, you will need to create a project from the badger archetype.

####Creating the Test Project


1) Create a new project by running this command on a folder that does not contains a pom.xml.   <br>
Use the most current Badger version for `archetypeVersion` :

```
mvn archetype:generate -DgroupId=com.dlouvton.<team-name> -DartifactId=<my-project-name> -DarchetypeVersion=<archetype-version> -DarchetypeGroupId=com.dlouvton.badger -DarchetypeArtifactId=badger-project-archetype -DinteractiveMode=false

for example:
mvn archetype:generate -DgroupId=com.dlouvton.badger -DartifactId=badger-itest -DarchetypeVersion=1.3-RELEASE -DarchetypeGroupId=com.dlouvton.badger -DarchetypeArtifactId=badger-project-archetype -DinteractiveMode=false
```

NOTE: In case you get a build failure, you may build it locally.

Build the archetype locally from `badger` directory:

```
cd archetype
mvn clean install
```

Repeat step 1 to create the Test Project. Do not specify -DarchetypeVersion=<archetype-version>.


2) A new folder was created for you, with the project files, with a sample test case.

Run the sample test case as following:
```
cd <my-project-name>
./run.sh
```

3) add other dependencies as required, for example TestNG and Selenium.

You may Add test cases. Your test case MUST extend BaseTest.java

```
import com.dlouvton.badger.itest.common.BaseTest;
...
public class ConnectivityTest extends BaseTest {
...
}
```

4) choose an existing model or add one, and edit your user.properties, to reflect the model that you'd like to use, and other configuration values.
 Note that if you ran the setup script, a user.properties file was generated for you. For example:

```
log-level=FINE
model-path=models/kafka-zk.json
post-to-mongo=false
private-key="dlouvton-key-pair"
private-key-path="./dlouvton-key-pair"
```

5) run the tests using the script run.sh.

6) You may create model, and vagrant configuration files (such as shell scripts, manifests and Vagrantfile) in the "stage" folder, a staging area.

when you run tests, these files will be copied from stage/models and stage/vagrant onto the models and vagrant folders.
When you're finished developing in the staging area, please contribute the files back to the Badger project, via a pull request.
