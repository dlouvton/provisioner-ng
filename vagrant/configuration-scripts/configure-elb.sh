#!/bin/bash
set -o pipefail

if [[ "$#" -lt 1 || "$#" -gt 2 || "$*" == *help* ]]; then
   echo "Usage: $0 CommaSeparatedList [Optional: ELBName]"
   echo "$0 create an Elastic Load Balancer to forward traffic from created public IP address and port to a aws internal port which a aws server can listen on."
   echo "CommaSeparatedPortList: Supply a list of PublicVipPort:InstancePort, comma separated, e.g. 80:8080,22:22"
   echo "If a second argument is passed, it will be used as the name for the ELB generated"
   exit 1
else
   portsList=$1
   IFS=',' read -a array <<< "$portsList"
fi

function waitFor {
   case $2 in
      ''|*[!0-9]*)  echo "Error: waitFor must accept a message and a number" >&2; exit 1;;
   *) ;;
   esac
      echo " -- Waiting until $1... ($2 seconds)" 1>&2
      sleep $2
      echo
}

envProps=/vagrant/.environment1.properties
source $envProps

echo "Finding ip..."
myip=`/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
echo "This machine's private ip is $myip"

cd ~/git
[ ! -d "rruc" ] && git clone https://github.com/jbasu/rruc.git
cd rruc

echo "Getting the instance id..."
myinstanceid=`./rruc.rb EC2 DescribeInstances | grep $myip | grep "running" | cut -d '|' -f 1 | tr -d ' '`
if [ -z $myinstanceid ]; then
   echo "Could not find instanceid, exiting"
   exit 1
fi
echo "Instance ID is $myinstanceid"
echo "instance_id=$myinstanceid" >> $envProps
echo $myinstanceid > /vagrant/.lastInstanceId

if [ -z "$2" ]; then
   elb="$USER-BadgerELB-`date +"%m%d%y-%H%M"`"
else
   elb=$2
fi

echo " -- Creating ELB $elb..."
listenerStr=""
for element in "${array[@]}"
do
    IFS=':' read -a ports <<< "$element"
    listener="protocol=TCP,lb-port=${ports[0]},instance-port=${ports[1]}"
    echo "Adding Listener $listener"
    listenerStr="$listenerStr --listener $listener"
done

echo " -- Running command rruc.rb ELB CreateLoadBalancer -ng $elb $listenerStr ..."
./rruc.rb ELB CreateLoadBalancer -ng $elb $listenerStr | tee elbCreation.out
elb_id=$(cat elbCreation.out| grep $elb | grep -o "$elb.*org.net")
if [ -z "$elb_id" ]; then
echo "Error getting elb_id, aborting...";
exit 1;
fi
echo
waitFor "ELB is successfully created" 10


echo " -- Registering instance $myinstanceid with $elb ELB"
./rruc.rb ELB RegisterInstancesWithLoadBalancer -ng $elb $myinstanceid | tee elbRegistration.out || exit 1
echo " -- Instance successfully registered with ELB $elb_id"

echo "Checking if host command is installed..."

if command -v host &>/dev/null; then
   echo "Host command found"
else
   echo "Host command not found, installing..."
   sudo yum -y install bind-utils &>/dev/null
fi

waitFor "IP to propagate" 30
echo " -- Getting a VIP by running command 'host $elb_id 10.224.57.8'..."
NEXT_WAIT_TIME=30
until host $elb_id 10.224.57.8 | tee host.out || [ $NEXT_WAIT_TIME -eq 900 ]; do
   echo " -- $NEXT_WAIT_TIME sec: public IP isn't ready, will retry in 30 seconds ...";
   sleep 30; 
   NEXT_WAIT_TIME=$((NEXT_WAIT_TIME+30))
done

elb_ip=$(grep "has address" host.out | awk '{print $4}')
if [ -z "$elb_ip" ]; then
   echo "was not able to retrieve a public IP using the host command, check with the aws team. exiting ..."
   echo "You may try this command manually: host $elb_id 10.224.57.8"
   exit 1
fi

echo "Tagging instance"
./rruc.rb EC2 CreateTags $myinstanceid "started_from","$HOSTNAME","provisioned_at","$(date +"%Y-%m-%d %H:%M")","elb_vip","$elb_ip","elb_id","$elb_id"

echo
echo " -- VIP $elb_ip was generated for ELB."
! [[  -z "$uniqueName" ]] && echo "$uniqueName"_elbip"=$elb_ip" >> $envProps && cat $envProps

