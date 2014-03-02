class tomcat(
  $version = '7.0.50',
  $owner   = 'root',
  $group   = 'root',
  $target  = '/mnt',
){

  $package_name    = "apache-tomcat-${version}"
  $service_code    = 'apache-tomcat'
  $tomcat_home     = "${target}/${package_name}"

  tag($service_code)

  file { "${target}/packs":
    ensure => directory,
  }

  exec {
    'Download tomcat package':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => "$target/packs",
      unless    => "test -f ${target}/${package_name}.tar.gz",
      command   => "wget -q ${package_repo}/${package_name}.tar.gz",
      logoutput => 'on_failure',
      creates   => "${local_dir}/${package_name}.tar.gz",
      require   => File["${target}/packs"];

    'Extract tomcat package':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      unless    => "test -d ${target}/${tomcat_home}/conf",
      command   => "tar xvfz ${target}/packs/${package_name}.tar.gz",
      logoutput => 'on_failure',
      creates   => "${target}/${tomcat_home}/conf",
      require   => Exec['Download tomcat package'];

    'Set tomcat home permission':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      command   => "chown -R ${owner} ${tomcat_home}; chmod 755 ${tomcat_home}",
      require   => Exec['Extract tomcat package'];

    'Start tomcat':
      path        => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd         => "${tomcat_home}/bin",
      environment => 'JAVA_HOME=/opt/java',
      command     => 'bash startup.sh',
      logoutput   => 'on_failure',
      require     => Exec['Set tomcat home permission'];
  }
}
