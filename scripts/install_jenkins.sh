vagrant_dir=../vagrant
cd $vagrant_dir
bundle
if [ $? -ne 0 ]; then 
	echo "ERROR: could not bundle vagrant on folder $PWD; This script must run inside aws"
	exit 1
fi

if ! grep jenkins__1 .vagrantuser; then  
echo " Updating $vagrant_dir/.vagrantuser"
echo "
model: none
components:
 jenkins__1:
  static: true
  expose: HostName, IPAddress
  role: jenkins
  name: jenkins__1" >> .vagrantuser
fi  
vagrant up jenkins__1
  
