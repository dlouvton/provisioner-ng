file {'test-dir':
  path => "/home/vagrant/blah",
  ensure => directory,
  owner => "vagrant",
  group => "vagrant",
}
