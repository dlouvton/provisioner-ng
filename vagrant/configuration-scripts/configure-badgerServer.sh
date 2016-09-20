#!/bin/bash
source /vagrant/.environment1.properties
echo " -- Installing/verifying pymongo..."
sudo easy_install pymongo 2>/dev/null

cd "$gitProjectPath"
./run.sh start-server

elb_id="badger-$uniqueName$RANDOM"
/vagrant/configuration-scripts/configure-elb.sh 80:8080,22:22 "$elb_id"
[ "$?" -ne "0" ] && exit 1

source /vagrant/.environment1.properties
projectName=$(basename "$gitProjectPath")
elb_ip_var=$uniqueName'_elbip'
elb_ip=${!elb_ip_var}
sudo yum -y -q install sendmail
date_str="$(date +"%Y-%m-%d-%H:%M")"
passphrase=$RANDOM

echo "install_type=server
elb_ip=$elb_ip
elb_id=$elb_id
user=$userEmail
server_id=$uniqueName
instance_id=$instance_id
key_pair=badger-automation-key-pair
started_from=$HOSTNAME
date=\"$date_str\"
project_path=$gitProjectPath
job_url=$url
passphrase=$passphrase">~/.badger-config

doc="{
\"user\" : \"$userEmail\",
\"install_type\" : \"SERVER\",
\"instance_id\" : \"$instance_id\",
\"elb_ip\" : \"$elb_ip\",
\"elb_id\" : \"$elb_id\",
\"server_id\" : \"$uniqueName\",
\"started_from\" : \"$HOSTNAME\",
\"key_pair\" :  \"badger-automation-key-pair\",
\"date\" : \"$date_str\",
\"project_path\" : \"$gitProjectPath\",
\"job_url\" : \"$url\",
\"passphrase\" : \"$passphrase\"
}"

echo " - Registering server: $doc"
cd "$gitProjectPath"
registration_id=$(scripts/mongo_access.py POST TestRunner "$doc")
[ -z "$registration_id" ] && "error registering the server" && exit 1
mailSubject="Badger Server for project $projectName was successfully created"
[ -n "$url" ] && urlMessage="Troubleshooting: to check a detailed log of the installation, visit $url/console"
echo "sending an email to $userEmail ($uniqueName)"
echo "From: Badger-Jenkins@dlouvton.com
To: $userEmail,dlouvton@dlouvton.com
Subject: $mailSubject
Content-Type: text/html
MIME-Version: 1.0

<html>
<head><title>$mailSubject</title>
</head>
<body>
<h2>$mailSubject</h2>
<h3>The server can be accessed at <a href=\"http://$elb_ip/v1/main/\">http://$elb_ip/v1/main</a></h3>

<p>Please follow these instructions:</p>

<ul>
	<li>Clone the <a href=\"$gitRepoUrl\">$projectName</a> git repository</li>
	<li>Register your remote server:
    <ul>
        <li><code>cd $projectName</code></li>
        <li><code>./run.sh -r \"$registration_id\" install</code></li>
    </ul>
</ul>
<p>That's all!</p>
<p>Use <b>run.sh</b> to login into the server or run tests, or <b>badgerCli</b> to interact with the server via API</p
<p>$urlMessage</p>
</body>
</html>
" > /tmp/email.txt
echo "email content:"
cat /tmp/email.txt
sendmail $userEmail  < /tmp/email.txt
localconfig=$(cat ~/.badger-config)
echo "

Please look for an email in your inbox. It might be in your spam folder.
if you did not receive the email, here are the instructions:

Clone the $gitRepoUrl git repository
cd $projectName
./run.sh -r \"$registration_id\" install


if you run into Python import issues, you may also run this command, to avoid using pymongo:

echo \"$localconfig
export BADGER_SERVER_URL=$elb_ip
\" > ~/.remote-badger-config


"

echo ... Complete!

