vagrant directory
=================
This directory contains the files needed to define the roles available within the Badger framework, and
spin up VM instances corresponding to those roles.

###Files here are on VMs too
When it creates a new VM, Vagrant automatically creates the `/vagrant` directory on the VM, and copies all
of the files that are in the same directory as the Vagrantfile into the new `/vagrant` directory.

Put another way, **any files in this directory will be present on the VMs created by Badger**.  You can
access these files on the VM in the `/vagrant` directory.

For this reason, files in this directory should be kept to a minimum in both size and quantity.

###Important files
 - install-rps-pkg.sh:  This is a script that installs the latest RPS package available from the RPS store.  
It is used during the setup of several roles.
 - Vagrantfile:  This is the source of truth for which roles are available in the system and how they are
configured.  A role must be present in this file to be created in aws.

###Directories
 - configuration-files:  This directory contains files used when configuring roles.  Examples of this could include base
properties files, tarballs containing required files, and so on.
 - manifests:  This directory contains the Puppet manifests used during the Install phase.  These manifests 
should prepare a base image to become a role-specific machine.  For example, these manifests can install RPS
packages or yum products, create directories, and so forth.  These manifests do not have access to the
environment properties while executing, and therefore **must not** be used for configuration of services.
 - modules:  This directory contains Puppet modules that are shared across multiple roles.  For example,
these modules could perform tasks like running `iptables --flush`.
 - test:  This directory contains files used by the Badger framework unit tests.
 - utils:  This directory contains support files used by the install-rps-pkg.sh script.
