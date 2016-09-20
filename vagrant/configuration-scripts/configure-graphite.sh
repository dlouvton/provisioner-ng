#!/bin/bash

# Load environment and configure network
source /vagrant/configuration-scripts/base-configure.sh
loadEnvironment /vagrant/$1 
renameHostname $2

# Clone ORG-specific git repos
cd $HOME
mkdir -p git
cd git
#git clone git@github.com:badgerInfra/carbon-org
#git clone git@github.com:badgerInfra/graphite-web-org
#Temporary workaround until graphite-aws python library deployment 
git clone git@github.com:rmarkowsky/temp
sudo cp /home/cloud-user/git/temp/lib_python26.tar /usr/lib
cd /usr/lib
sudo tar -cf python26.backup.tar python2.6/
sudo tar -xf lib_python26.tar

sudo chown -R cloud-user:cloud-user /var/lib/carbon/
sed -i 's/info/debug/g' /home/cloud-user/kmcg/templates/config/log4j.properties.template
sed -i 's/warn/debug/g' /home/cloud-user/kmcg/templates/config/log4j.properties.template

###########################
# Start graphite services
cd $HOME/kmcg/build
echo "jvmargs.Xms=-Xms2048M" >> user.properties
echo "jvmargs.Xmx=-Xms2048M" >> user.properties

echo "*** Starting Graphite at $(date) as $(whoami) in workingdir $(pwd) with homedir $HOME..."

sudo service carbon-cache start
sudo service httpd start
echo "*** Starting Graphite consumer... (results in startResults.txt)"
./ant start > startResults.txt 2>&1
echo "*** Complete!"

