# -*- mode: ruby -*-
# vi: set ft=ruby :

include base

file {'Linux-dir':
	path => "/home/cloud-user/dev/tools/Linux/",
	ensure => directory,
	owner => "cloud-user",
	group => "cloud-user",
	require => Class['base'],
}

file {'jdk-dir':
	path => "/home/cloud-user/dev/tools/Linux/jdk",
	ensure => directory,
	owner => "cloud-user",
	group => "cloud-user",
	require => File['Linux-dir'],
}

file {'java-symlink':
	path => "/home/cloud-user/dev/tools/Linux/jdk/jdk1.7.0_45_x64",
	ensure => link,
	target => "/usr/",
	owner => "cloud-user",
	group => "cloud-user",
	require => File['jdk-dir'],
}

base::installer {'graphite-consumer-install':
   product => "badger-graphite-kafka-consumer",
   role => "kmcg",
   dir_name => "kmcg",
   require => File['java-symlink'],
}

file { '/home/cloud-user/kmcg/templates/config/server.properties.template':
     ensure  => present,
     source  => '/home/cloud-user/kmcg/templates/config/server.properties.dev.template',
     require=>base::installer['graphite-consumer-install'],
}

file { '/home/cloud-user/kmcg/templates/config/site-list.xml.template':
     ensure  => present,
     source  => '/home/cloud-user/kmcg/templates/config/site-list.xml.dev.template',
     require=>base::installer['graphite-consumer-install'],
}


$packages = ['graphite-web', 'carbon', 'whisper', 'httpd', 'django-tagging']

package { $packages:
  ensure  => latest,
  require => Yumrepo['isd-base'],
}

exec { 'change_owner':
     command => "/bin/sed -i -e  's/USER = carbon/USER = cloud-user/' /etc/carbon/carbon.conf",
     onlyif => "/bin/grep -c 'USER = carbon' /etc/carbon/carbon.conf",
     require   => Package['carbon'],
}

file { '/var/log/carbon-cache':
     owner => 'cloud-user',
     ensure => directory,
     require => Package['carbon'],
}
