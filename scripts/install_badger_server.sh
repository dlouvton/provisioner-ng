#!/bin/sh
function usage {
        echo "USAGE: install_badger_server.sh <userEmailAddress> <uniqueName> <git project path> <optional url> <optional git repo>"
        echo "<userEmailAddress> valid email address where you will receive instructions how to access your server"
        echo "<uniqueName> the server identifier. can only have a-z, A-Z and 0-9 characters. max 20 characters"
        echo "<git project path> location of badger project on the server, e.g. $HOME/git/badger-itest"
        echo "<optional url> jenkins build url for troubleshooting"
        echo "<optional git repo url> git repo url for troubleshooting"
        exit 2
}

if [ "$#" -lt 3 ] ; then
    echo "Illegal number of parameters"
    usage
fi

userEmail=$1
uniqueName=$2
gitProjectPath=$3
url=$4
gitRepoUrl=$5

if ! [[ "$uniqueName" =~ ^[a-zA-Z0-9/-][-a-zA-Z0-9]{0,20}[a-zA-Z0-9]$ ]] ; then
    echo "You must provide a valid unique name"
    usage
fi

if ! [[ "$userEmail" =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,4}$ ]] ; then
    echo "You must provide a valid email address"
    usage
fi

vagrant_dir=../vagrant
cd $vagrant_dir
bundle  > /dev/null 2>&1
if [ $? -ne 0 ]; then
        echo "ERROR: ** This script must run inside aws **"
        echo "could not bundle vagrant on folder $PWD; Please do not run this script on your local workstation. You may ssh into a aws machine in order to run this script."
        exit 3
fi

if ! grep badgerServer__1 .vagrantuser; then
cp .vagrantuser .vagrantuser.copy
echo "Updating $vagrant_dir/.vagrantuser. server settings are saved at .vagrantuser.server"
compName=badgerServer_"$uniqueName"__1
echo "
model: none
components:
 $compName:
  static: true
  expose: HostName, IPAddress
  role: badgerServer
  name: $compName" >> .vagrantuser
fi
echo "userEmail=$userEmail
uniqueName=$uniqueName
url=$url
gitProjectPath=$gitProjectPath
gitRepoUrl=$gitRepoUrl">.environment1.properties
vagrant up $compName
[ "$?" -ne "0" ] && exit 1

cp .vagrantuser .vagrantuser.server
mv .vagrantuser.copy .vagrantuser
