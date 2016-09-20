# -*- mode: ruby -*-
# vi: set ft=ruby :

include base

base::installer {'zookeeper-install':
   product => "badger-zookeeper",
   role => "badger-zookeeper",
   dir_name => "zookeeper",
}

exec {'zookeeper-config':
  # Note here that I picked "relango-vml" at random, since it's already in the file.
  # There is nothing special about that machine name, other than it's always there.
  # Basically the EnvironmentConfig.groovy file just needs to have the current hostname in it,
  # in a particular format.  Rather than try and figure out how to create a new entry and
  # force it to be in the right place, I just edit an existing one.
  #
  # This is likely to not be the right long-term solution; a sentinel value specifically
  # for this purpose would be better.
  command => "sed -i s/relango-vml/${hostname}/g /home/cloud-user/zookeeper/config/EnvironmentConfig.groovy",
  path => "/bin",
  require => base::installer['zookeeper-install'],
}

