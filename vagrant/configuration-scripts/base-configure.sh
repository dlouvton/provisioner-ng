#!/bin/bash

configure_debug_output=1

# $1 == Filename to load; usually /vagrant/environment.properties
# $2 == Whether to print debug output or not.  Pass 1 for yes, any other value for no.  Defaults to no, unless specified elsewhere in this file.
function loadEnvironment() {

	if [ "$1" == "" ]; then
		echo "ERROR:  Must pass a filename to function loadEnvironment!"
		exit 99
	fi

	if [ "$2" != "" ]; then
		configure_debug_output=$2
	fi

	debugEcho "Loading environment..."
	debugEcho "Current host is `hostname`"
	debugEcho "Current user is `whoami`"
	debugEcho "Current home directory is $HOME"
	debugEcho ""
	debugEcho "Contents of file:"
	debugEcho "===================================================="
	debugEcho "$( cat $1 )"
	debugEcho "===================================================="
	debugEcho ""
	debugEcho "Sourcing file..."
	source $1
	echo "source $1">>$HOME/.bashrc
	addAllHostEntries $1
}

# $1 == Hostname to add.  Required.
# $2 == IP address to associate with that hostname.  Required.
# $3 == Whether to print debug output or not.  Pass 1 for yes, any other value for no.  Defaults to no, unless specified elsewhere in this file.
function addHostFileEntry() {

        if [ "$1" == "" ] || [ "$2" == "" ]; then
                echo "ERROR:  Must pass both a hostname and IP address to function addHostFileEntry!"
                exit 99
        fi

        if [ "$3" != "" ]; then
                configure_debug_output=$3
        fi

	debugEcho "Adding hostname $1 with IP $2 to /etc/hosts"
	sudo puppet apply -e "host { '$1':    name => '$1',     ip => '$2',    ensure => present,  }" > /dev/null 2>&1
}

function debugEcho() {
	if [ "$configure_debug_output" == "1" ]; then
		echo "$*"
	fi
}


# Read the env properties and add entries for all lines that match <component>_HostName and <component>_AliasHostName
# for example, for line kafka_HostName=ip-192-168-1-134, it will add the entry ip-192-168-1-134=> 192.168.1.134
# the IP address is retrieved by sourcing env.properties and evaluating $<component>_IPAddress

function addAllHostEntries() {
	regex="(.*)_.*HostName=(.*)"
	while IFS= read -r line
	do
	    if [[ $line =~ $regex ]]; then
		    componentName="${BASH_REMATCH[1]}"
		    newHostName="${BASH_REMATCH[2]}"
		    ip=$componentName'_IPAddress'
		    addHostFileEntry $newHostName ${!ip}
		    addHostFileEntry $componentName ${!ip}
		fi
	done < $1
}

# The function takes a <componentName> (i.e. kafka) as its only argument
# If the variable <componentName>_AliasHostName is defined, rename the hostname to it, otherwise rename hostname to <componentName>

function renameHostname() {
	if [ -z $1 ]; then
		echo "Skipping renaming component name; no argument was passed into renameHostname()"
		exit 0
	fi
	alias=$1'_aliasHostName'
	if [ -z ${!alias} ]; then
		echo "Renaming hostname to component name $1"
		sudo hostname $1
	else
		echo "Renaming the component $1 hostname to ${!alias}"
		sudo hostname ${!alias}
	fi
}
