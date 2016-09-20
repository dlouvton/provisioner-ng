function error_exit
{
        echo "Exiting, $1" 1>&2
        exit 1;
}

nodejsbin=~/node-v0.10.33-linux-x64/bin
vagrant_dir=../vagrant
cd $vagrant_dir
bundle
if [ $? -ne 0 ]; then
        echo "ERROR: could not bundle vagrant on folder $PWD; This script must run inside aws"
        exit 1
fi

if ! grep dashboard__1 .vagrantuser; then
	echo " Updating $vagrant_dir/.vagrantuser"
	echo "
model: none
components:
 dashboard__1:
  static: true
  expose: HostName, IPAddress
  role: base
  name: dashboard__1" >> .vagrantuser
fi
vagrant up dashboard__1 || error_exit "failed to start dashboard vm"
vagrant ssh dashboard__1 -c "sudo iptables -F; cd; wget http://nodejs.org/dist/v0.10.33/node-v0.10.33-linux-x64.tar.gz;tar -xvf node-v0.10.33-linux-x64.tar.gz" || error_exit "failed to download node.js"
vagrant ssh dashboard__1 -c "cd; mkdir git; cd git; rm -rf badger-dash; git clone https://github.com/dlouvton/badger-dash.git" || error_exit "failed to clone badger-dash"
vagrant ssh dashboard__1 -c "cd ~/git; rm -rf badger; git clone https://github.com/dlouvton/badger.git" || error_exit "failed to clone badger"
vagrant ssh dashboard__1 -c "cd ~/git/badger; nohup mvn clean compile exec:java 2>&1 | tee out.log &" || error_exit "failed to start badger"
vagrant ssh dashboard__1 -c "cd ~/git/badger-dash; $nodejsbin/npm install; nohup $nodejsbin/npm start &" || error_exit "failed to clone badger-dash"
vagrant ssh dashboard__1 -c "/vagrant/configuration-scripts/configure-elb.sh 22:22,80:8086,8080:8080,9090:9090" || error_exit "failed to create ELB"
echo "Dashboard was installed successfully"
