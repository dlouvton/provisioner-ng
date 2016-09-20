#!/bin/bash
set -o pipefail
source $PWD/scripts/include.sh
envID=$(echo "$@" | grep -o 'envID=.*')
if [ -z "$envID" ]; then
    echo "ERROR: must pass an envID (e.g. envID=1)"
    exit
else
    echo "Running tests via API using option $envID"
fi
log=$PWD/test.log
mvn $badger_mvn_args clean verify "-D$envID" >&1 | tee $log
exitCode=$?

result=$([ "$exitCode" == 0 ] && echo "Successfully" || echo "with failures (exit code: $exitCode)")
echo "Test run completed $result"

DATESTRING=`date +"%Y-%m-%d-%H_%M"`
echo "Copying console output to logs/$DATESTRING.log"
echo "Copying TestNG report to logs/$DATESTRING.html"

failsafe_reports=$PWD/target/failsafe-reports
logs=$PWD/logs
mkdir -p $logs
cp $failsafe_reports/emailable-report.html $logs/$DATESTRING.html 2>&1 > /dev/null
exit $exitCode

