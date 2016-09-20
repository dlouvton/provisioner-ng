# -*- mode: ruby -*-
# vi: set ft=ruby :

include base

base::installer {'kafka-install':
   product => "badger-kafka",
   role => "kafka",
   dir_name => "kafka",
}


