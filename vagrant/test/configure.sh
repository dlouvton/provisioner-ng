#!/bin/bash

# Diagnostic information
echo ======================================================
echo Running configure.sh on machine `hostname` at `date`...
echo
echo The passed parameter is $1
echo
echo Sourcing /vagrant/$1...

source /vagrant/$1

echo
echo The contents of /vagrant/$1 are: 

cat /vagrant/$1

echo ======================================================
echo

# Actual script starts here
echo "configuring host with hostName=$host"
echo
