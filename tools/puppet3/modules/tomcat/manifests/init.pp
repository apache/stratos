# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

class tomcat(
  $owner   = 'root',
  $group   = 'root',
  $target  = '/mnt',
){

  require java
  class {'python_agent':}

  $package_name    = "apache-tomcat-${tomcat_version}"
  $service_code    = 'apache-tomcat'
  $tomcat_home     = "${target}/${package_name}"

  tag($service_code)

  file { 
#    "${target}/packs":
#      ensure => directory;

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

  # install stratos_base before java before tomcat before agent
  Class['stratos_base'] -> Class['java'] -> Class['python_agent'] -> Class['tomcat']
}

