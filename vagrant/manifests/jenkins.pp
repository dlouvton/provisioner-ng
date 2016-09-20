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

yumrepo { "jenkins-repo":
  baseurl  => "http://pkg.jenkins-ci.org/redhat",
  enabled  => 1,
  gpgcheck => 0,
  require  => Exec['selinux-enforce-off'],
}

# Jenkins needs to be able to navigate to scripts under /home/cloud-user/git/badger
file { '/home/cloud-user':
  ensure => directory,
  mode => 0755,
}

package { 'jenkins':
  ensure  => latest,
  require => Yumrepo['jenkins-repo'],
}

service { 'jenkins':
  ensure => running,
  enable => true,
  require => Package['jenkins'],
}

exec { 'install-badger-script':
  command => "/home/cloud-user/installBadger.sh",
  cwd => "/home/cloud-user",
  user => "cloud-user",
  environment => ["HOME=/home/cloud-user"],
  timeout => 0,
  require => Service['jenkins'],
}
