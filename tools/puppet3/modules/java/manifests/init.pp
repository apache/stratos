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

class java { 

  $java_home  = "/opt/${java_name}"
  $package  = $java_distribution
  $local_dir = $local_package_dir

  file {
    "/opt/${package}":
      ensure => present,
      source => "puppet:///modules/java/${package}",
      mode   => '0755',
      ignore => '.svn';

    '/opt/java':
      ensure  => link,
      target  => "${java_home}",
      require => Exec['Install java'];

    '/etc/profile.d/java_home.sh':
      ensure   => present,
      mode     => '0755',
      content  => template('java/java_home.sh.erb');
  }
    
  exec { 
     'Install java':
      path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      cwd     => '/opt',
      command => "/bin/tar xzf ${package}",
      unless  => "/usr/bin/test -d ${java_home}",
      creates => "${java_home}/COPYRIGHT",
      require => File["/opt/${package}"];
  }
}
