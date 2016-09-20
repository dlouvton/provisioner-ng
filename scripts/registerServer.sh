#!/bin/bash
set -o pipefail
source $PWD/scripts/include.sh
$PWD/scripts/mongo_access.py FINDBYID TestRunner "{}" "$1" | tee registration_output
[ "$?" != 0 ] && echo "Error registering server $1" && exit 1
mv registration_output ~/.remote-badger-config
source ~/.remote-badger-config
echo "export BADGER_SERVER_URL=\"$elb_ip\"">> ~/.remote-badger-config
echo "Registered server $1 successfully"
