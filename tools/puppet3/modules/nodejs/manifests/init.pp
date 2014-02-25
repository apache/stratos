class nodejs {

  $target = '/mnt'

  if $stratos_app_path {
    $nodejs_home = $stratos_app_path
  } 
  else {
    $nodejs_home = "${target}/nodejs"
  }

  package { ['python-software-properties', 'python', 'g++', 'make']:
    ensure => installed,
  }


  exec {
    'update-apt':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'apt-get update > /dev/null 2>&1';

    'add-repo':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'add-apt-repository ppa:chris-lea/node.js > /dev/null 2>&1';

    'Create nodejs home':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${nodejs_home}";
    
    'Install libraries':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd     => "${nodejs_home}",
      command => 'apt-get install nodejs',
      require => [
        Exec['update-apt'], 
        Package['python-software-properties', 'python', 'g++', 'make'],
	Exec['add-repo'],
	Exec['update-apt'],
	Exec['Create nodejs home'],
      ];

    'Start application':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => "${nodejs_home}",
      onlyif    => 'test -f web.js',
      command   => 'node web.js > /dev/null 2>&1 &',
      tries     => 100,
      try_sleep => 2,
      require   => Exec['Install libraries'];
  }
}

