#!/bin/bash

# Load environment and configure network
source /vagrant/configuration-scripts/base-configure.sh
loadEnvironment /vagrant/$1 

# Stop Jenkins
echo Stopping Jenkins...
sudo service jenkins stop

# Remove temp files
TEMPDIR=/tmp/jenkinstmp
if [ -d $TEMPDIR ]; then
        sudo rm -rf $TEMPDIR
fi

# Create temp dir
mkdir -p $TEMPDIR

# Refresh git
echo Updating Git files...
cd ~/git
git clone https://github.com/dlouvton/badger-jenkins.git
cd badger-jenkins
git pull origin > $TEMPDIR/gitPull.txt 2>&1

# Copy everything to temp
echo Updating Jenkins files...
cp -r ~/git/badger-jenkins/config/* $TEMPDIR

# Change ownership of temp files
sudo chown -R cloud-user:cloud-user $TEMPDIR
sudo chown -R cloud-user:cloud-user /var/lib/jenkins
sudo chown -R cloud-user:cloud-user /var/log/jenkins
sudo chown -R cloud-user:cloud-user /var/cache/jenkins

# Copy files into place, maintaining ownership
sudo cp -rp $TEMPDIR/* /var/lib/jenkins

echo "Changing jenkins user..."
sudo sed -i "s/JENKINS_USER=[^ ]*/JENKINS_USER=cloud-user/g" /etc/sysconfig/jenkins

# Start jenkins
echo Starting Jenkins...
sudo service jenkins start

/vagrant/configuration-scripts/configure-elb.sh 80:8080,22:22 "$USER-BadgerJenkins-`date +"%m%d%y-%H%M"`"

date_str="$(date +"%Y-%m-%d %H:%M")"
echo "install_type=jenkins
user=badgerJenkins
started_from=$HOSTNAME
provisioned_at=\"$date_str\"">~/.badger-config

echo ... Complete!

