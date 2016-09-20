# -*- mode: ruby -*-
# vi: set ft=ruby :

class dns {

	# External resources
	host { 'localhost':
	  ensure       => 'present',
	  host_aliases => ['localhost'],
	  ip           => '127.0.0.1',
	  target       => '/etc/hosts',
	}


	# VMs
	host { 'tower':
	  ensure       => 'present',
	  host_aliases => ['tower'],
	  ip           => '10.10.10.100',
	  target       => '/etc/hosts',
	}

	host { 'kafka':
	  ensure       => 'present',
	  host_aliases => ['kafka','badger-kafka'],
	  ip           => '10.10.10.150',
	  target       => '/etc/hosts',
	}

	host { 'graphite':
	  ensure       => 'present',
	  host_aliases => ['graphite'],
	  ip           => '10.10.10.200',
	  target       => '/etc/hosts',
	}

    # RPS VMs
    host { 'kafka2':
	  ensure       => 'present',
	  host_aliases => ['kafka2'],
	  ip           => '10.10.10.50',
	  target       => '/etc/hosts',
	}

        host { 'kafka3':
          ensure       => 'present',
          host_aliases => ['kafka3'],
          ip           => '10.10.10.51',
          target       => '/etc/hosts',
        }

        host { 'zookeeper1':
          ensure       => 'present',
          host_aliases => ['zookeeper1'],
          ip           => '10.10.10.59',
          target       => '/etc/hosts',
        }

        host { 'kafka4':
          ensure       => 'present',
          host_aliases => ['kafka4'],
          ip           => '10.10.10.60',
          target       => '/etc/hosts',
        }

        host { 'kafka5':
          ensure       => 'present',
          host_aliases => ['kafka5'],
          ip           => '10.10.10.61',
          target       => '/etc/hosts',
        }

        host { 'zookeeper6':
          ensure       => 'present',
          host_aliases => ['zookeeper6'],
          ip           => '10.10.10.69',
          target       => '/etc/hosts',
        }

				host { 'kafka7':
          ensure       => 'present',
          host_aliases => ['kafka-secure-sec1'],
          ip           => '10.10.10.70',
          target       => '/etc/hosts',
        }

        

}
