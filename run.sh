#!/bin/bash
CONFIG_FILE=~/.badger-config
REMOTE_CONFIG_FILE=~/.remote-badger-config
IS_NOT_BUILT=false
[ -f $CONFIG_FILE ] && source $CONFIG_FILE
[ -f $REMOTE_CONFIG_FILE ] && source $REMOTE_CONFIG_FILE

# write key value pairs to the main config file
function update_config {
    KEY=$1
    VALUE=$2
    if [ -z "$(grep $KEY= $CONFIG_FILE)" ]; then
            printf "$KEY=\"$VALUE\"\n" >> $CONFIG_FILE
    else
            sed  -ie "s/\($KEY*=*\).*/\1\"$VALUE\"/" $CONFIG_FILE
    fi
}


# update from git and fetch the dependencies from Nexus
function build {
    git pull --ff-only > /dev/null 2>&1
    export TAG_USERNAME=$user
    badger_version=${1:-$(grep override-pom-badger-version user.properties | sed -e 's/override-pom-badger-version=//' -e 's/\s*#.*$//')}

    echo "Cleaning up old logs and artifacts"
    rm -rf target models vagrant scripts default.properties out.log test-output

    exitCode=1
    badger_mvn_args=" -q -U "

    if [ -z "$badger_version" ] ; then
       echo "Compiling"
       mvn -q clean compile 2>&1
       exitCode=$?
    else
    if ! [[ "$badger_version" =~ ^[0-9]+.[0-9]+-(RELEASE|SNAPSHOT)$ ]]; then
          echo "Check: invalid Badger version $badger_version"
          exit;
       fi

       echo "Compiling from Badger version $badger_version ...";
       badger_mvn_args=" $badger_mvn_args -Dbadger-jar-version=$badger_version -Dbadger-common-files-version=$badger_version "
       mvn compile $badger_mvn_args
       exitCode=$?
    fi
    
    if [ "$exitCode" != 0 ]; then
        echo
        echo "Error installing Badger using maven. "
        echo "Please make sure that you have 'mvn' installed, and that you have the ability to pull artifacts from dlouvton Nexus."
        exit 1
    fi
    
    update_config badger_mvn_args "$badger_mvn_args"
    chmod +x scripts/*
    echo "Build successful"
    return $exitCode
}

# script usage
function usage {
    echo "
Usage: `basename $0` [-h] [-v version] [-e envID] [-r remote-id] [ACTION]

-h help
-v badger-version: override the default Badger version on pom.xml, e.g. 1.5-SNAPSHOT
-e envID (default 1): use the envID when running tests
-r remote-server-id: provide one in order to register a remote Badger server.

ACTION:

Basic Actions:
        test (default): run included testNG tests locally. You must be on the remote server to run this command.
        install (or build): update from git and compile project
        remote-run: sync and run tests remotely
        ssh: ssh into the remote server. [optional: <command to run>]

Advanced Actions:
        remote-install: install a remote test runner on aws (Note: it's recommended to install a test runner from Jenkins)
        remote-destroy: destroy the remote test runner on aws 
        test-via-api: run tests without provisioning
        start-server: start Badger as an API server [optional: port=<server port>, default 8080]
        remote-sync: sync the currect directory with the remote server (copying local files to the remote server)
        remote-start-server: start the remote Badger API server
        config: show local and remote config

For example:
 - To run tests againts 1.5-SNAPSHOT: './run.sh -v 1.5-SNAPSHOT'
 - To register a remote server: './run.sh -r <registration_id> register'
 - To run tests remotely, update tests and the stage folder locally, and run: './run.sh remote-run'
 - TO run a command on the remote server and exit: ./run.sh ssh <command>

Note: If you're using Badger as an API server, it's recommended to run this command 'export BADGER_SERVER_URL=<server-ip>', and use the CLI program badgerCli.
for more info, run './badgerCli -h'"

    exit;
}

# main program where cmd line arguments are parsed and passed to the appropriate script
if [[ $( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd ) == *badger ]]; then
    echo "This script cannot run on this directory"
    exit
fi
# Parse command line options.
while getopts hv:e:r: OPT; do
    case "$OPT" in
        h)
            usage
            ;;
        e)
            ENV_ID=${OPTARG}
            ;;
        r)
            REMOTE_ID=${OPTARG}
            ;;
        v)
            BADGER_VERSION=${OPTARG}
            ;;
        \?)
            usage
            ;;
    esac
done

shift $((OPTIND-1))

if [ $# -eq 0 ]; then
    ACTION="test"
else
    ACTION=$1
fi

[ -z "$badger_mvn_args" ] && IS_NOT_BUILT=true
[ -z "$ENV_ID" ] && ENV_ID=1

case "$ACTION" in
        install|build|register)
            build $BADGER_VERSION
            [ -z "$REMOTE_ID" ] && exit $?
            echo "Registering server. You may have to provide your sudo password."
            pip install pymongo > /dev/null
            [ "$?" != 0 ] && sudo easy_install pymongo 1> /dev/null
            scripts/registerServer.sh $REMOTE_ID || exit 1
            source $REMOTE_CONFIG_FILE
            ping -c 3 $elb_ip > /dev/null 2>&1
            [ $? -ne 0 ] && echo "Connection Error: Server is registered but it cannot be reached at $elb_ip" && exit 1
            exit 0
            ;;
        test)
            build $BADGER_VERSION
            scripts/runTests.sh $BADGER_VERSION
            exit $?
            ;;
        test-via-api)
            $IS_NOT_BUILT && build $BADGER_VERSION
            scripts/runApi.sh $BADGER_VERSION envID=$ENV_ID
            exit $?
            ;;
        start-server)
            build $BADGER_VERSION
            scripts/runServer.sh $BADGER_VERSION
            exit $?
            ;;
        remote-start-server)
            scripts/remote-run.sh start-server
            exit $?
            ;;
        ssh)
            [ -z "$elb_ip" ] && echo "No registered server found" && exit 1
            [ -z "$key_pair" ] && echo "No key pair found" && exit 1
            key_path=~/git/rruc/"$key_pair"
            [ "$key_pair" == "badger-automation-key-pair" ] && key_path=$PWD/vagrant/badger-automation-key-pair
            [ ! -f "$key_path" ] && echo "Could not find the key pair $key_path. Please copy the key pair to this location and try again" && exit 1
            chmod 600 $key_path
            ssh -o 'StrictHostKeyChecking no' -i $key_path cloud-user@$elb_ip "$2"
            exit $?
            ;;
        remote-install)
            scripts/remote-run.sh install $BADGER_VERSION
            exit $?
            ;;
        remote-destroy)
            scripts/remote-run.sh destroy $BADGER_VERSION
            exit $?
            ;;
        remote-run)
            scripts/remote-run.sh rsync
            scripts/remote-run.sh run $BADGER_VERSION
            exit $?
            ;;
        remote-sync)
            scripts/remote-run.sh rsync
            exit $?
            ;;
        config)
            printf "Local Config\n-----------\n"
            cat $CONFIG_FILE 2> /dev/null
            printf "\nRemote Config\n-----------\n"
            cat $REMOTE_CONFIG_FILE 2> /dev/null
            exit $?
            ;;
        *)  echo "$ACTION : Not Available"
            usage
           ;;
esac
