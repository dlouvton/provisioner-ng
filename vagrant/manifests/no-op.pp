# -*- mode: ruby -*-
# vi: set ft=ruby :

include base

exec {'no-op-install':
  command => "/bin/echo 'no-op puppet manifest; doing nothing...'",
}

