#!/bin/sh
LOCAL_CONFIG_FILE=~/.badger-config
REMOTE_CONFIG_FILE=~/.remote-badger-config
CONFIG_FILE=$LOCAL_CONFIG_FILE

[ -f $LOCAL_CONFIG_FILE ] && source $LOCAL_CONFIG_FILE
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

function update_remote_config {
    CONFIG_FILE=$REMOTE_CONFIG_FILE
    update_config $1 $2
}
