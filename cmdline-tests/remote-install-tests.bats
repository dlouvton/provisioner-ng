#!/usr/bin/env bats

interactive=false

@test "remote testrunner installation via cmdline" {
[ $skip_remote_install == "true" ] || [ $remote_install_method != "cmdline" ] && skip "skip_remote_install=$skip_remote_install, remote_install_method=$remote_install_method"
    CAN_I_RUN_SUDO=$(sudo -n uptime 2>&1|grep "load"|wc -l)
    if [ ! ${CAN_I_RUN_SUDO} -gt 0 ] && [ "$interactive" == "false" ]; then
       echo "you have to have sudo to run this test unintended" && bats_error_trap
       exit 1
    else
        cd tmp
        run bash -c "./run.sh remote-install <<< $'\n' || true"
    fi
}

@test "remote testrunner installation via jenkins" {
[ $skip_remote_install == "true" ] || [ $remote_install_method != "jenkins" ] && skip "skip_remote_install=$skip_remote_install, remote_install_method=$remote_install_method"
    lastBuildNumber=$(curl $JENKINS_URL/job/install-badger-server/lastBuild/buildNumber)
    nextBuildNumber=$((lastBuildNumber+1))
    curl -X POST http://$JENKINS_URL/job/install-badger-server/build \
        --user badger:123 \
        --data-urlencode json='{"parameter": [{"name":"EMAIL", "value":"jenkins@$USER.org"}, {"name":"PROJECT_ROOT", "value":"/home/cloud-user/git/badger-itest"}, {"name":"GIT_REPO_URL", "value":"https://github.com/dlouvton/badger-itest"}]}'

    NEXT_WAIT_TIME=30
    sleep 10
    curl -f $JENKINS_URL/job/install-badger-server/$nextBuildNumber/consoleText
    [ "$?" != 0 ] && echo "Build is not successful" && exit 1
    until registration_line=$(curl $JENKINS_URL/job/install-badger-server/$nextBuildNumber/consoleText | grep  -m1 "run.sh -r") || [ $NEXT_WAIT_TIME -eq 600 ]; do
        echo " -- $NEXT_WAIT_TIME sec: server is not ready ..."
        sleep 30
        NEXT_WAIT_TIME=$((NEXT_WAIT_TIME+30))
    done

    export registration_id=$(echo $registration_line | awk '{print $3}')
    if [ -z $registration_id ]; then
        echo "Installation failed - could not obtain a registration id from jenkins" && bats_error_trap
        exit 1
    fi

    cd tmp
    run bash -c "./run.sh -r $registration_id register"
    [ $status -eq 0 ]
}

# in order to run this test run command 'export remote_registration_id=<registration_id>
@test "remote testrunner registration" {
[ -z "$remote_registration_id" ] && skip "not required, remote_registration_id is not set"
    cd tmp
    run bash -c "./run.sh -r $remote_registration_id register"
    [ $status -eq 0 ]
}
