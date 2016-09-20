#!/bin/bash
# This script creates an instance on aws, where you could run Badger tests. It will attempt to install Ruby for you, but if its not working 
# you can run the script with the option VM to install Ruby on a vagrant VM.
# once Ruby is installed, it will clone the RRUC project (a aws cmd-line tool), and will create the necessary credentials and unique key to use it.
# RRUC will create an instance for you, and will attach an ELB (Elastic Load Balancer) to it so you can access it from the corp network.

PS1="$ " #So we can source bashrc
source ~/.bashrc
PS1=""
if [ -z $BADGER_UPDATE_BASHRC ]; then
   BADGER_UPDATE_BASHRC=1
fi

#First arg: string to append
#(Optional) second arg: file to append to; if $2 not present, append to ~/.bashrc
function append_to_bashrc { 
   bashrc="$HOME/.bashrc"
   
   if [ $# -eq 2 ]; then
      bashrc=$2
   fi   

   if [ "$BADGER_UPDATE_BASHRC" != 0 ]; then
   		 echo $1 >> $bashrc
   fi
}
export -f append_to_bashrc
function usage {
	echo "USAGE: install_aws_instance.sh <unique_name> [OPTIONAL: SERVER|JAR] [OPTIONAL: VM|CLEAN]"
    echo "<unique name> (i.e. dlouvton) this will be used to create your key-pair on aws"
    echo "(optional) <SERVER|JAR>: install badger as an API server, testNG tests do not run. If you don't choose this option, or choose JAR, badger will be installed as a local jar and testNG tests will run"
    echo "(optional) <VM|CLEAN>: VM: install ruby on a vagrant box (not recommended, unless you absolutely cannot install Ruby 2.0 on your box); CLEAN: Reinstall Ruby and RVM"
}

if [ "$#" -lt 1 ] ; then
    echo "Illegal number of parameters"
    usage
    exit 1;
fi

if ! [[ -z "$2"  || "$2" =~ ^(SERVER|JAR)$ ]]; then
      echo "Check: invalid optional option $2."
      usage
      exit 2;
fi
   
if [[ "$3" == "VM" ]] ; then
    echo "installing a Ruby vagrant Box ... it is not recommended unless you absolutely cannot install Ruby 2.0 locally";   
    if [[ $(vagrant -v) != *Vagrant* ]]; then
          echo "vagrant is not installed, please install it manually and retry";
          echo "vagrant: $(vagrant -v)"
		  exit 3;
    fi
    if [[ $(vboxmanage -v) != *4.* ]]; then
          echo "Error: virtualbox 4.2 is not installed, please install it manually and retry";
          echo "vboxmanage: $(vboxmanage -v)"
          exit 4;
    fi
    
    if [ -d ~/ruby-box ]; then
    	  echo "Destroying existing VM..."
    	  pushd ~/ruby-box
    	  vagrant destroy -f > /dev/null 2>&1
    	  popd
    	  rm -rf ~/ruby-box
    fi
    
    echo " -- Downloading a vagrant Ruby-Box ..."
    git clone https://github.com/rails/rails-dev-box.git
    mv rails-dev-box ~/ruby-box
    cp install_remote_aws_testrunner.sh ~/ruby-box
    cd ~/ruby-box
    echo " -- Starting the Ruby-Box ..."
    vagrant up
    echo " -- Installing the aws Test Runner from the Ruby-Box ..."
    vagrant ssh -c "/vagrant/install_remote_aws_testrunner.sh $1"
    echo
    echo "-------------------------------------------------------------------------------"
    echo "IMPORTANT: You must be logged into the Ruby box BEFORE executing commands above"
    echo "-------------------------------------------------------------------------------"
    echo 'Ruby box is up!'
    echo 'To log onto the Ruby VM:  cd ~/ruby-box; vagrant ssh'
    echo 'See above messages for the address of your aws machine.'
    exit 0;
fi

if [[ "$3" == "CLEAN" ]] ; then
  echo " -- Will attempt to reinstall ruby locally, be prepared to enter sudo password. This might not work on Mac";
  echo " -- removing rvm ..."
  sudo gem uninstall -q fog
  sudo rm -rf $HOME/.rvm $HOME/.rvmrc /etc/rvmrc /etc/profile.d/rvm.sh /usr/local/rvm /usr/local/bin/rvm
  sudo /usr/sbin/groupdel rvm
  echo " -- RVM is removed. Please check all .bashrc|.bash_profile|.profile|.zshrc for RVM source lines and delete or comment out if this was a Per-User installation."   
  echo " -- Installing ruby and rvm ..."
  \curl -sSL https://get.rvm.io | bash
  source $HOME/.rvm/scripts/rvm
  echo " -- Installing ruby 2.0.0 ..."
  append_to_bashrc "[[ -s \"$HOME/.rvm/scripts/rvm\" ]] && source \"$HOME/.rvm/scripts/rvm\""
  append_to_bashrc "rvm use --install 2.0.0"
  rvm use --install 2.0.0
  if [ $? -ne 0 ]; then echo " -- Error: failed to install ruby, try to run this script with the option VM. in extreme cases you might have to rm -rf /opt/rubies and remove ruby manually. aborting..."; exit 1; fi
fi

if [[ $(ruby -v) != *ruby?2.* ]]; then
  echo "Ruby 2.0+ is required"
  echo "Current Ruby version: $(ruby -v)"
  echo "Please run './install_aws_testrunner.sh $1 <JAR|SERVER> CLEAN' or './install_aws_testrunner.sh $1 <JAR|SERVER> VM', aborting..."
  exit 1;
fi 

echo " -- Installing the aws Test Runner from your localbox ..."

echo " -- Installing Badger $2 ..."
./install_remote_aws_testrunner.sh $1 $2 || exit 1
append_to_bashrc "export BADGER_UPDATE_BASHRC=0" 