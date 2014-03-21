class tomcat(
  $owner   = 'root',
  $group   = 'root',
  $target  = '/mnt',
){

  $package_name    = "apache-tomcat-${tomcat_version}"
  $service_code    = 'apache-tomcat'
  $tomcat_home     = "${target}/${package_name}"

  tag($service_code)

  file { 
    "${target}/packs":
      ensure => directory;

    "${tomcat_home}/conf/server.xml":
      ensure   => present,
      content  => template('tomcat/server.xml.erb'),
      require  => Exec['Extract tomcat package'];
  }

  file {
    "/${target}/packs/apache-tomcat-${tomcat_version}.tar.gz":
      ensure => present,
      source => "puppet:///modules/tomcat/apache-tomcat-${tomcat_version}.tar.gz",
      require => File["${target}/packs"];
  }

  file { '/mnt/tomcat':
    content => template('tomcat/tomcat.erb'),
    require => File["${target}/packs"];
  }

  exec {
    'Extract tomcat package':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      unless    => "test -d ${target}/${tomcat_home}/conf",
      command   => "tar xvfz ${target}/packs/${package_name}.tar.gz",
      logoutput => 'on_failure',
      creates   => "${target}/${tomcat_home}/conf",
      require   => File["/${target}/packs/apache-tomcat-${tomcat_version}.tar.gz"];
	
    'Set tomcat home permission':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      command   => "chown -R ${owner} ${tomcat_home}; chmod -R 755 ${tomcat_home}",
      require   => [
        Exec['Extract tomcat package'],
        File["${tomcat_home}/conf/server.xml"],
      ];

    'Start tomcat':
      path        => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd         => "${tomcat_home}/bin",
      environment => 'JAVA_HOME=/opt/java',
      command     => 'bash startup.sh',
      logoutput   => 'on_failure',
      require     => Exec['Set tomcat home permission'];
  }
}

