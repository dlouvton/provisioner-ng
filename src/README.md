src directory
=============
The src directory contains, unsurprisingly, the source for Badger.  Badger is written in Java, and contains 
a few non-Java resources as well.

This directory does not contain the Vagrantfile or its related items (Puppet manifests, shell setup scripts, 
and so forth).

###Directories
 - badger:  This directory contains the Java source code for the Badger framework.  This includes 
the provisioner and its related components, but does not include things like model or property files.
 - resources:  This directory includes the model files that represent environments for Badger to create, 
files that are used by Badger's unit tests, and files that are needed for jar packaging and test execution.
 - test:  This directory contains the unit tests for the Badger framework.
 
