#!/usr/bin/env bats

@test "ssh test" {
    cd tmp
    run ./run.sh ssh ls
    [[ $output == *"rruc"* ]]
}

@test "remote sync" {
    cd tmp
    run ./run.sh remote-sync
    [[ $output == *"Sync completed"* ]]
}

@test "remote run" {
    cd tmp
    bash -c  "./run.sh remote-run |tee out || true"
    run cat out
    [[ $output == *"Tests run: 2, Failures: 1, Errors: 0, Skipped: 0"* ]]
    rm out
}

@test "restart remote server" {
    cd tmp
    bash -c "./run.sh remote-start-server | tee out || true"
    run cat out
    [[ $output == *"Badger server was installed successfully"* ]]
    rm out
}
