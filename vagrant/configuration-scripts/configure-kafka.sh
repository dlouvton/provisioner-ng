#!/bin/bash

# Load environment and configure network
source /vagrant/configuration-scripts/base-configure.sh
loadEnvironment /vagrant/$1
renameHostname $2

# Start the kafka service
cd /home/cloud-user/kafka/build
echo "environment=$environmentName" >> user.properties

echo "*** Starting Kafka at $(date) as $(whoami) in workingdir $(pwd) with homedir $HOME... (results in startResults.txt)"
./ant start > startResults.txt 2>&1

# Now we have to create the topics that we need, before any other components/listeners come up
# If listeners are allowed to come up BEFORE these topics are created, then they will not
# register the existence of those topics until they are restarted.  This will cause all
# functionality in integration tests to fail, since those components are not restarted after 
# the tests start running, as that would just be silly.

# This is a limitation of vanilla Kafka 0.8, and is not restricted to our ORG-customized version.

# THIS IMPLIES THAT YOU HAVE TO KNOW THE TOPICS YOU ARE USING IN YOUR TESTS AHEAD OF TIME.
# IT ALSO IMPLIES THAT YOU HAVE TO CREATE THOSE TOPICS HERE.
# THUS, IF YOU ADD NEW TESTS THAT USE NEW TOPICS, YOU **MUST** EDIT THIS SCRIPT TO CREATE SAID TOPIC.
# I have no idea what the best way to advertise this is, and I expect new test cases to run into this a lot.

# A space-delimited set of topics to create; ADD YOUR TOPICS HERE!
TOPICLIST="badger-automation-poc-topic FunnelProto com.dlouvton.badger.ajna.Metric.System com.dlouvton.badger.ajna.Metric.SAN.Array"

echo "*** Creating topics ahead of first use (logs in $HOME)..."

# The script is in for example /home/cloud-user/installed/badger-kafka__main__8727342_Linux.x86_64.internal.runtime.kafka_DEFAULT/tools/kafka/kafka-0.8.0-SNAPSHOT-org-0.7/bin
# That is not under the symlink /home/cloud-user/kafka :-(
# So we have to go through a fairly crazy process to figure out that directory, because it changes
SYMLINK=`readlink -f /home/cloud-user/kafka`
create_topic_script=./manageTopics.sh
if [ -d "$SYMLINK/../tools/kafka" ]; then
  create_topic_script=./kafka-create-topic.sh
  cd $SYMLINK/../tools/kafka
  cd `ls -d */`
  cd bin
else
  cd $SYMLINK/bin
fi

echo "Creating topics using script $PWD/$create_topic_script"

for TOPIC in $TOPICLIST
do
	echo "   *** Creating topic $TOPIC ..."
	$create_topic_script --topic $TOPIC --zookeeper $zookeeper_HostName:2181 > $HOME/createTopic-$TOPIC.log 2>&1
	echo "   *** Done."
done



echo "*** Complete!"

