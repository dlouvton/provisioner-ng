define base::installer ($product = 'none', $role = 'none', $timeout = 0, $dir_name = 'none',
						$base_dir = '/home/cloud-user', $log = '/vagrant/install-logs') {
	 #install_from is coming from puppet.facter in Vagrantfile
	 #the format for install_from is repo::url::branch or rps::manifest-or-branch
	 #e.g. git::https://github.com/badgerApp/badger-radio-relay::jetty, rps::freeze or rps::badger-agent__freeze-98108.rmf  

	 $location = split($install_from, '::')
	 # repo is the repository type, e.g. rps or git
	 $repo = $location[0]
	 # location_arg1 could be git url, rps manifest file, or rps branch
	 $location_arg1 = $location[1]
	 # location_arg2 could be git branch
	 $location_arg2 = $location[2]
	 alert("installing from location $install_from")
	 package {'unzip':
		ensure => present,
		require => Class['base'],
	 }

	if $repo == 'rps' {
		if $location_arg1 =~ /.*rmf$/ {
		       alert("Running rps installation from manifest $location_arg1")
		       exec {'rps-install-from-manifest':
		              command => "/vagrant/install-rps-pkg.sh -p $product -m $location_arg1 -r $role -d $dir_name -h $base_dir -l $log",
		              cwd => "/vagrant",
		              user => "cloud-user",
		              timeout => $timeout,
		              require => package['unzip'],
		              logoutput => true,
		            }
	   	} else {
		       alert("Running rps installation from branch $location_arg1")
		       exec {'rps-install':
		              command => "/vagrant/install-rps-pkg.sh -p $product -b $location_arg1 -r $role -d $dir_name -h $base_dir -l $log",
		              cwd => "/vagrant",
		              user => "cloud-user",
		              timeout => $timeout,
		              require => package['unzip'],
		              logoutput => true,
		            }
		}
	} elsif $repo  == 'git' {
		      alert ( "Running git installation from $location_arg1/$location_arg2" )
		      exec {'git-install':
		              command => "/vagrant/install-git-pkg.sh $location_arg1 $location_arg2 $base_dir/$dir_name",
		              cwd => "/vagrant",
		              user => "cloud-user",
		              timeout => $timeout,
		              require => package['unzip'],
		              logoutput => true,
		            }
	} else {
			fail ('the repo $repo is not supported (should be git or rps). install_from should have the format git::url::branch or rps::manifest-or-branch')
	}
}
