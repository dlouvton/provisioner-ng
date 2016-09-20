#!/bin/sh
git_project_url=$1
git_branch=$2
dir_name=$3

echo " --- git cloning $git_project_url, branch $git_branch, into folder $dir_name ..."
rm -rf $HOME/$dir_name
git clone $git_project_url --branch $git_branch $HOME/$dir_name

cd $HOME/$dir_name/build



if [ ! -f build.xml ]; then
    echo "Could not find an ant build project. "
    exit
fi

echo " --- updating user.properties ..."
echo "
JAVA_HOME=/usr/lib/jvm/jre-openjdk
ANT_HOME=/home/cloud-user/dev/tools/ant/apache-ant-1.8.3
MAVEN_HOME=/home/cloud-user/dev/tools/maven/apache-maven-3.0.4
KAFKA_HOME=/home/cloud-user/tools/kafka/kafka-0.8.0-SNAPSHOT-org-0.8
">>user.properties

echo " --- running ant package (result in ~/antPackage.txt) ..."
./ant package | tee ~/antPackage.txt

result=$(grep 'BUILD SUCCESSFUL' ~/antPackage.txt)
if [ -z "$result" ]; then
 echo
 echo "Failed to package $dir_name successfully..."
 exit 99
fi

echo " --- git installation completed successfully!"
exit 0
