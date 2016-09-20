Provisioner-ng
==============

Provisioner-ng, provides the framework to deploy environments and run integration tests against multi-node software on AWS and other cloud providers.

###Develop Provisioner-ng
To contribute to Provisioner-ng, you may clone this project and import the maven project into Eclipse and install the Eclipse TestNG plugin. Once the project is imported, you can run tests as TestNG tests or execute the goal "mvn test".
Note that if you use your Eclipse setup, you will not be able to create instances on aws or access instances, but it might be useful for some tasks.

Pre-requisites: make sure you have maven installed.

```
git clone https://github.com/dlouvton/provisioner-ng.git
cd Provisioner-ng
mvn install
```
To import the project to Eclipse, go to File / Import / Maven/ Existing Maven Project, and browse to the Provisioner-ng folder.
To add the TestNG plugin to Eclipse:
Go to Help / Software updates / Find and Install, and enter http://beust.com/eclipse. Once the plugin is added restart Eclipse.

###AWS Setup
If you'd like to run the tests on aws, you may have to install a Test Runner by following these directions, and then ssh into the newly created instance.




###Developing and Running Tests
In order to develop an integration test in the Provisioner-ng framework, you will need to create a project from the Provisioner-ng archetype.

####Creating the Test Project


1) Create a new project by running this command on a folder that does not contains a pom.xml.   <br>
Use the most current Provisioner-ng version for `archetypeVersion` :

```
mvn archetype:generate -DgroupId=com.dlouvton.<team-name> -DartifactId=<my-project-name> -DarchetypeVersion=<archetype-version> -DarchetypeGroupId=com.dlouvton.Provisioner-ng -DarchetypeArtifactId=Provisioner-ng-project-archetype -DinteractiveMode=false

for example:
mvn archetype:generate -DgroupId=com.dlouvton.Provisioner-ng -DartifactId=Provisioner-ng-itest -DarchetypeVersion=1.3-RELEASE -DarchetypeGroupId=com.dlouvton.Provisioner-ng -DarchetypeArtifactId=Provisioner-ng-project-archetype -DinteractiveMode=false
```

2) A new folder was created for you, with the project files, with a sample test case.

Run the sample test case as following:
```
cd <my-project-name>
./run.sh
```

3) add other dependencies as required, for example TestNG and Selenium.

You may Add test cases. Your test case MUST extend BaseTest.java

```
import com.dlouvton.Provisioner-ng.itest.common.BaseTest;
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
private-key-path="./dlouvton-key-pair"
```
