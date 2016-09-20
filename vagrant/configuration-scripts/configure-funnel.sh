#!/bin/bash

# Load environment and configure network
source /vagrant/configuration-scripts/base-configure.sh
loadEnvironment /vagrant/$1
renameHostname $2

# configure funnel
propertiesFile="funnel.$environmentName.properties"
cd /home/cloud-user/funnel/config
echo "
kafkaBrokerHost=$kafka_IPAddress
kafkaBrokerSecure=true
funnelGatewayMaxThreads=100" >> $propertiesFile

cd /home/cloud-user/funnel/build
echo "environment=$environmentName" >> user.properties

# Start the funnel service

cd /home/cloud-user/funnel/build

echo "*** Starting funnel at $(date) as $(whoami) in workingdir $(pwd) with homedir $HOME..."

echo "*** Starting funnel... (results in startResults.txt)"
./ant start > startResults.txt 2>&1

funnelURL="http://$funnel_HostName:8080/funnel/v1/publishBatch?avroSchemaFingerprint=AVG7NnlcHNdk4t_zn2JBnQ&debug=true"

echo "Funnel URL is $funnelURL "
sleep 5

curl -i -X POST -H 'Content-type: application/json' -d '[{"service":"System","tags":{"source":"Tower","datacenter":"WAS","superpod":"SP3","pod":"NA12","device":"na12-app1-2-was.ops.org.net"},"timestamp":1360294220,"metricName":["cpu","core-1","idle"],"metricValue":7.0}]' $funnelURL | tee curlResults.txt

# expecting 200
if [ -z "$(grep '200 OK' curlResults.txt)" ] ; then
   echo "Error verifying funnel ...";
   exit 1;
fi

echo "*** Complete!"
