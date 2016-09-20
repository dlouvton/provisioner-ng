# -*- mode: ruby -*-
# vi: set ft=ruby :

# This class contains things that need to be executed on all roles.
class base {

  # Necessary because otherwise /etc/hosts does not contain an entry for the machine's hostname
  # Without that entry, various things fail (such as Java calls to getHostName, etc)
  host { 'localhost':
    name => "${hostname}",
    ip => "${ipaddress}",
    ensure => present,
  }

  if $operatingsystem == 'RedHat' {
	# Necessary because by default, RHEL boxes are set to not allow any inbound connections.
	# That's troublesome for, you know, pretty much everything we are doing.
	exec {'iptables-flush':
	  command => "iptables --flush",
	  path => "/sbin",
	}
  }
}

