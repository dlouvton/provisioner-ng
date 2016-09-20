#!/bin/bash
set -o pipefail
source $PWD/scripts/include.sh
mvn $badger_mvn_args clean verify 2>&1 | tee out.log
exitCode=$?

DATESTRING=`date +"%Y-%m-%d-%H_%M"`
result=$([ "$exitCode" == 0 ] && echo "Successfully" || echo "with failures (exit code: $exitCode)")
echo "Test run completed $result"
echo "Copying console output to logs/$DATESTRING.log"
echo "Copying TestNG report to logs/$DATESTRING.html"

failsafe_reports=target/failsafe-reports
mkdir -p logs
mv out.log logs/$DATESTRING.log
cp $failsafe_reports/emailable-report.html logs/$DATESTRING.html 2>&1 > /dev/null
exit $exitCode
