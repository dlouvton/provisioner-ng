#!/bin/sh
if [ "$#" -lt 1 ]; then
  echo "
Error: illegal number of parameters. Please provide the badger version, e.g. 1.5-SNAPSHOT
Usage: cmdline-tests.sh BADGER-VERSION [ OPTIONAL: List of suites (double quoted) ]
examples:  cmdline-tests.sh 1.5-SNAPSHOT : runs all tests against 1.5-SNAPSHOT
           cmdline-tests.sh 1.5-SNAPSHOT run-local-tests.bats : run only run-local-tests.bats tests

"
  exit 1;
fi


export badger_version=$1
if ! [[ "$badger_version" =~ ^[0-9]+.[0-9]+-(RELEASE|SNAPSHOT)$ ]]; then
          echo "Check: invalid Badger version $badger_version"
          exit;
fi

all_tests="setup.bats local-tests.bats remote-install-tests.bats remote-tests.bats badgerCli-tests.bats teardown.bats"
list_of_suites=${2:-$all_tests}
export model=base
export skip_remote_install=false
# remote_install_method could be jenkins or cmdline
export remote_install_method=jenkins
export skip_teardown=false

#load .config to override settings
source ./.config 2> /dev/null

if ! [ -e bats ]; then
    git clone https://github.com/sstephenson/bats.git || exit 1
    cd bats/
    ./install.sh /usr/local 2> /dev/null
    if [ "$?" -ne 0 ]; then
        sudo ./install.sh /usr/local
        [ "$?" -ne 0 ] && echo "could not install bats" && exit 1
    fi
    cd ..
fi

echo "Running the following test suites: $list_of_suites"

/usr/local/bin/bats $list_of_suites
