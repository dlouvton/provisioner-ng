#!/usr/bin/env bats

@test "destroy test runner" {
    [ $skip_teardown == "true" ] && skip "skip_teardown=true"
    cd tmp
    run ./run.sh remote-destroy
    [ $status -eq 0 ]
}

