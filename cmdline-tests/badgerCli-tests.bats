#!/usr/bin/env bats
model=base

@test "check badgerCli help" {
    cd tmp
    run ./badgerCli -h
    [[ $output == *"usage"* ]]
}

@test "API: listModels" {
    cd tmp
    run ./badgerCli listModels
    [[ $output == *"base.json"* ]]
}

@test "API: listEnvironments (0)" {
    cd tmp
    run ./badgerCli listEnvironments
    [[ $output == *"numberOfEnvironments\":\"0\""* ]]
}

@test "API: describeLog" {
    cd tmp
    run ./badgerCli describeLog
    [[ $output == *"Initiating Jersey application"* ]]
}

@test "API: describeVersion" {
    cd tmp
    run ./badgerCli describeVersion
    [[ $output == *"Badger Integration Testing Framework"* ]]
}

@test "API: start model $model" {
    cd tmp
    run ./badgerCli -m $model.json start
    [[ $output == *"environmentStatus\":\"BUSY"* ]]
}

@test "API: waitUntilEnvironmentReady, wait until ready" {
    cd tmp
    run ./badgerCli waitUntilEnvironmentReady
    [[ $output == *"Status Code: 200"* ]]
}

@test "API: describeProperties" {
    cd tmp
    run ./badgerCli describeProperties
    [[ $output == *"$model_HostName"* ]]
}

@test "API: runTests" {
    cd tmp
    run ./badgerCli runTests
    [[ $output == *"Tests run: 2, Failures: 1, Errors: 0, Skipped: 0"* ]]
}

@test "API: describeScripts" {
    cd tmp
    run ./badgerCli describeScripts
    [[ $output == *"scriptsLocation"* ]]
}

@test "API: runScript no arguments" {
    cd tmp
    run ./badgerCli runScript
    [[ $output == *"'script' and 'component' must be provided for the runScript action"* ]]
}

@test "API: runScript example.sh" {
    cd tmp
    run ./badgerCli runScript -c base -s example.sh
    [[ $output == *"Running a example script"* ]]
}

@test "API: postToMongo no arg" {
    cd tmp
    run ./badgerCli postToMongo
    [[ $output == *"'file_to_upload' must be provided for the postToMongo action"* ]]
}

@test "API: describeStatus" {
    cd tmp
    run ./badgerCli describeStatus
    [[ $output == *"systemStatus\":\"READY"* ]]
}

@test "API: destroy" {
    cd tmp
    run ./badgerCli destroy
    [[ $output == *"numberOfEnvironments\":\"0"* ]]
}

@test "API: forceDestroyModel" {
    cd tmp
    run ./badgerCli forceDestroyModel
    [[ $output == *"numberOfEnvironments\":\"0"* ]]
}

@test "API: forceDestroyAll" {
    cd tmp
    run ./badgerCli forceDestroyAll
    [[ $output == *"numberOfEnvironments\":\"0"* ]]
}



