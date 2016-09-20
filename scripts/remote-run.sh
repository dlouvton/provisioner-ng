#!/bin/bash
source $PWD/scripts/include.sh
shopt -s expand_aliases
action=$1
badger_version=$2
rsync_args="-aqpL --progress -e"
JENKINS_URL=""
pathToEnv="$HOME/git/rruc/env.sh"
testDir=$(echo "$PWD" | sed -e "s|$HOME/||")
remoteTestDir="/home/cloud-user/$testDir"

function main {
   [ -e $pathToEnv ] && source $pathToEnv
   [ "$action" == "install" ] && install_runner

   [ -z "$BADGER_SERVER_URL" ] && echo "No Badger Server found. Have you created a Badger server from $JENKINS_URL? " && exit 2
   runnerIP=cloud-user@"$BADGER_SERVER_URL"
   keyPairPath=$PWD/vagrant/$key_pair
   [ -e "$HOME/git/rruc/$key_pair" ] && keyPairPath=$HOME/git/rruc/$key_pair
   chmod 600 $keyPairPath

   [ "$action" == "start-server" ] && start-server
   [ "$action" == "rsync" ] && remote_sync
   [ "$action" == "run" ] && run
   [ "$action" == "destroy" ] && destroy_runner
}

function install_runner {
   echo "Are you sure you want to create one using Vagtant and Ruby? "
   echo "It's much easier to install a test runner from $JENKINS_URL."
   echo
   read -p "Press [Enter] key to if you choose to proceed with a local installation that requires Ruby ... You may Ctrl-C to abort."
   if [ ! -d "scripts" ]; then
      echo "Did not find scripts folder, this script expects to be in the base directory of the test project"
      echo "Please run mvn compile before running this script, or move this script to the base directory before running"
      exit 1
   fi

   cd scripts
   keyName="badger-$USER$RANDOM"
   ./install_aws_testrunner.sh $keyName SERVER

   if [ $? != 0 ]; then
      echo "Testrunner installation failed with default option."
      echo "You may try this option: cd scripts; ./install_aws_testrunner.sh $keyName CLEAN"
      exit 1
   fi

   echo "New test runner is up!"
   source $pathToEnv
   cd ..
}

function destroy_runner {
   [ -z "$instance_id" ] && echo "could not find an instance_id to destroy, is the test runner running?" && exit 1
   scripts/mongo_access.py DELETE TestRuner {} $_id || exit 1
   ssh -i $keyPairPath $runnerIP "cd $project_path/scripts; ./terminateAllInstances.sh; cd /home/cloud-user/git/rruc 2>/dev/null || cd /home/cloud-user/rruc/ 2>/dev/null; ./rruc.rb ELB DeleteLoadBalancer $elb_id; ./rruc.rb EC2 TerminateInstances $instance_id"
   exitCode=$?
   [ $exitCode != 0 ] && echo "TestRunner Destruction failed for instance $instance_id and ELB $elb_id" && exit 1
   echo "Test Runner was destroyed successfully"
}

function remote_sync {
   echo "Rsyncing your project directory $PWD with $remoteTestDir at $runnerIP ..."
   ssh -o StrictHostKeyChecking=no -i $keyPairPath $runnerIP "mkdir -p $remoteTestDir"
   rsync $rsync_args "ssh -i $keyPairPath" $PWD $runnerIP:$remoteTestDir/..

   if [ $? != 0 ]; then
      echo "Rsync failed! Exiting"
      exit 1
   fi
   echo "Sync completed"
}

function run {
   echo "Running your tests on the remote server as $runnerIP (from $remoteTestDir) ..."
   if [ ! -z "$badger_version" -a "$badger_version" != " " ]; then
           echo "Using Badger version $badger_version"
           version_arg=" -v $badger_version"
   fi
   chmod 600 $keyPairPath
   [ "$?" != 0 ] && echo "Error accessing $keyPairPath" && exit 1

   sshv="ssh -o StrictHostKeyChecking=no -i $keyPairPath $runnerIP"
   echo "Using remote user $($sshv whoami)"
   [ "$?" != 0 ] && echo "Error connecting to TestRunner $runnerIP using private key $keyPairPath" && exit 1

   $sshv "cd $remoteTestDir; ./run.sh$version_arg" 2>/dev/null

   failsafeReports="$remoteTestDir/target/failsafe-reports"
   logs="$remoteTestDir/logs"
   finishOutput="Complete!"
   echo "Rsyncing results back from the test runner..."
   if $sshv '[ -d '"$failsafeReports"' ]'; then
      rsync $rsync_args "ssh -i $keyPairPath" $runnerIP:$failsafeReports $PWD
      rsync $rsync_args "ssh -i $keyPairPath" $runnerIP:$logs $PWD
      finishOutput="$finishOutput You can find the reports of your testrun in the target/failsafe-reports directory."
      [ -d "target/failsafe-reports" ] && rm -rf $failsafeReports
   else
      echo "Did not find the failsafe reports directory, likely because the build phase failed and tests never ran, just rsyncing the log back..."
      rsync $rsync_args "ssh -i $keyPairPath" $runnerIP:$logs $PWD
   fi

   echo "$finishOutput You can find the log of the run in logs/`ls -tr logs | tail -1`"
}

function start-server {
   ssh -o StrictHostKeyChecking=no -i $keyPairPath -t $runnerIP "cd $remoteTestDir; ./run.sh start-server"
}

main
