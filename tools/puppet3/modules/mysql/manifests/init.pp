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

class mysql{

  $custom_agent_templates = ['extensions/instance-started.sh']
  class {'python_agent':
    custom_templates => $custom_agent_templates,
    module=>'mysql'
  }

  if $stratos_mysql_password {
    $root_password = $stratos_mysql_password
  }
  else {
    $root_password = 'root'
  }

  package { ['mysql-server','phpmyadmin','apache2']:
    ensure => installed,
  }

  service { 'mysql':
    ensure  => running,
    pattern => 'mysql',
    require => Package['mysql-server'],
  }

  service { 'apache2':
    ensure  => running,
    pattern => 'apache2',
    require => Package['apache2'],
  }

#  exec { 'Set root password':
#    path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#    unless  => "mysqladmin -uroot -p${root_password} status",
#    command => "mysqladmin -uroot password ${root_password}",
#    require => Service['mysqld'];
#  }
#
#  if $root_password {
#    exec {
#      'Delete anonymous users':
#        path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#        command => "mysql -uroot -p${root_password} -Bse \"DELETE from mysql.user WHERE password=''\"",
#        require => Exec['Set root password'];
#
#      'Create mysql user root@%':
#        path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
#        command => "mysql -uroot -p${root_password} -Bse \"CREATE USER 'root'@'%' IDENTIFIED BY '${root_password}'\"",
#        require => Exec['Delete anonymous users'];
#    }
#  }

  file { '/etc/apache2/conf.d/phpmyadmin.conf':
    ensure  => present,
    content => template('mysql/phpMyAdmin.conf.erb'),
    notify  => Service['apache2'],
    require => [
      Package['phpmyadmin'],
      Package['apache2'],
    ];
  }

  file { '/etc/mysql/my.cnf':
    ensure  => present,
    content => template('mysql/my.cnf.erb'),
    notify  => Service['apache2'],
    require => [
      Package['phpmyadmin'],
      Package['apache2'],
    ];
  }

  file { '/etc/apache2/sites-enabled/000-default':
    content => template('mysql/000-default.erb'),
    notify  => Service['apache2'],
    require => [
      Package['phpmyadmin'],
      Package['apache2'],
    ];
  }

  exec { 'Restart MySQL' :
    path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
    command => "/etc/init.d/mysql restart",
    require => File['/etc/apache2/sites-enabled/000-default'];
  }

  # install stratos_base before java before mysql before agent
  Class['stratos_base'] -> Class['python_agent'] -> Class['mysql'] 
}
