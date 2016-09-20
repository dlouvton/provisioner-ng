# -*- mode: ruby -*-
# vi: set ft=ruby :

# Be sure to require Class['base'] for the first step in your logical chain
include base

# SELinux makes things not work; rather than investigate why, just turn it off
# It's not on in production anywhere, so this seems innocuous enough
exec { 'selinux-enforce-off':
     command => '/usr/bin/sudo /bin/sh -c "/bin/echo 0 > /selinux/enforce"',
     require  => Class['base'],
}

file { '/home/cloud-user':
  ensure => directory,
  mode => 0755,
}


exec { 'install-badger-script':
  command => "/home/cloud-user/installBadger.sh \"https://github.com/dlouvton/badger-itest.git\" \"badger-itest\"",
  cwd => "/home/cloud-user",
  user => "cloud-user",
  environment => ["HOME=/home/cloud-user"],
  timeout => 0,
}
