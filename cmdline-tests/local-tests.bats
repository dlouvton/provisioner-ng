#!/usr/bin/env bats

@test "check local config after build with version" {
    cd tmp
    run ./run.sh config
    [[ $output == *"badger-common-files-version=$badger_version"* ]]
}

@test "registration error" {
    cd tmp
    run ./run.sh -r wrong_id install
    [[ $output == *"Error registering server wrong_id"* ]]
}

@test "local run" {
    cd tmp
    run ./run.sh
    [[ $output == *"Tests run: 2, Failures: 1, Errors: 0, Skipped: 0"* ]]
}

@test "start server" {
[ $USER == "cloud-user" ] && skip "skip on jenkins"
    cd tmp
    ./run.sh start-server port=8082 > out.test
    # have to kill processes since bats for some reason will hang without it
    killall -9 java
    run cat out.test
    [[ $output == *"Badger server was installed successfully"* ]]
    rm out.test
}

@test "run via api" {
    cd tmp
    run ./run.sh -e 1 test-via-api
    [[ $output == *"Tests run: 2, Failures: 1, Errors: 0, Skipped: 0"* ]]
}

