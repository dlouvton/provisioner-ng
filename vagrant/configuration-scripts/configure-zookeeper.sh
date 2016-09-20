#!/bin/bash

# Load environment and configure network
source /vagrant/configuration-scripts/base-configure.sh
loadEnvironment /vagrant/$1
renameHostname $2

# Start zookeeper service
cd /home/cloud-user/zookeeper/build
echo "environment=$environmentName" >> user.properties

echo "*** Starting Zookeeper at $(date) as $(whoami) in workingdir $(pwd) with homedir $HOME... (results in startResults.txt)"
./ant start > startResults.txt 2>&1

echo "*** Complete!"
