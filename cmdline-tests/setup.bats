#!/usr/bin/env bats

@test "install archetype onto tmp" {
    rm -rf tmp
    mvn archetype:generate -DgroupId=com.dlouvton.badger -DartifactId=tmp -DarchetypeVersion=$badger_version -DarchetypeGroupId=com.dlouvton.badger -DarchetypeArtifactId=badger-project-archetype -DinteractiveMode=false
    # the verification does not work on redhat
    # [[ $output == *"BUILD SUCCESS"* ]]
    chmod +x tmp/run.sh
}

@test "build with version $badger_version" {
    cd tmp
    run ./run.sh -v $badger_version build
    [ $status -eq 0 ]
}
