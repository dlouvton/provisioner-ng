#!/bin/bash
set -o pipefail
source $PWD/scripts/include.sh
PORT=$(echo "$@" | grep -o 'port=.*')
[ -z "$PORT" ] && PORT=8080
kill -9 $(fuser -n tcp $PORT 2> /dev/null) 2> /dev/null
[ "$USER" == "cloud-user" ] && sudo iptables -F
echo " - Killing all java processes" && killall -9 java 2> /dev/null
echo " - Starting Badger Server locally on port $PORT..."
nohup mvn $badger_mvn_args exec:java 2>&1> out.log &
pid=$!

echo " - Waiting for Jetty to start"
NEXT_WAIT_TIME=5
until curl -I -L "http://localhost:$PORT/v1/main" 2>/dev/null | grep "HTTP.*200.*OK" || [ $NEXT_WAIT_TIME -eq 60 ]; do
   echo " -- $NEXT_WAIT_TIME sec: jetty is not ready on port $PORT, will retry in 5 seconds ..."
   sleep 5
   NEXT_WAIT_TIME=$((NEXT_WAIT_TIME+5))
done

if [ $NEXT_WAIT_TIME -ge 60 ]; then
   echo
   cat out.log
   echo " -- Failed to start Jetty successfully on port $PORT... Make sure that you run './run.sh build' first, and that the version badger-project-pom in your pom.xml is at least 1.4-SNAPSHOT. As a last resort, you may try 'killall -9 java'"
   kill -9 $pid
   exit 2;
fi

echo " - Badger server was installed successfully. PID is $pid. running on port $PORT."
[ "$USER" != "cloud-user" ] && echo " - WARNING: You are using user $USER. For the Badger server to function properly, it needs to be installed on aws using the cloud-user user"
