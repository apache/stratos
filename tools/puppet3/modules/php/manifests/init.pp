#--------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#--------------------------------------------------------------


class php () {

  $custom_agent_templates = ['extensions/artifacts-updated.sh']
  class {'python_agent':
    custom_templates => $custom_agent_templates,
    module=>'php'
  }

  $packages = [    
    'build-essential',
    'mysql-client',
    'apache2',
    'php5',
    'php5-cli',
    'libapache2-mod-php5',
    'php5-gd',
    'php5-mysql',
    'php-db',
    'php-pear',
    'php5-curl',    
    'php5-ldap',
    'php5-adodb',
    'mailutils',
    'php5-imap',
    'php5-sqlite',
    'php5-xmlrpc',
    'php5-xsl',
    'openssl',
    'ssl-cert',
    'ldap-utils',
    'php5-mcrypt',
    'mcrypt',
    'ufw',
    'fail2ban',
    'git',
    'libboost-all-dev',
    'ruby']

  file { '/etc/apt/apt.conf.d/90forceyes':
    ensure => present,
    source => 'puppet:///modules/php/90forceyes';
  }

  exec { 'update-apt':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'apt-get update > /dev/null 2>&1',
    logoutput => on_failure,
    require   => File['/etc/apt/apt.conf.d/90forceyes'];
  }

  package { $packages:
    ensure   => installed,
    provider => apt,
    require  => Exec['update-apt'],
  }

  exec { 'clean sites':
      path    => ['/bin', '/usr/bin', '/usr/sbin/'],
      command => 'rm -rf /etc/apache2/sites-available/*; rm -rf /etc/apache2/sites-enabled/*',
      require => Package['apache2'];
  }

  file {
   '/etc/apache2/apache2.conf':
      owner   => 'root',
      group   => 'root',
      mode    => '0775',
      content => template('php/apache2/apache2.conf.erb'),
      require => Package['apache2'];

    '/etc/apache2/sites-available/default':
      owner   => 'root',
      group   => 'root',
      mode    => '0775',
      content => template('php/apache2/sites-available/default.erb'),
      require => Exec['clean sites'];

    '/etc/apache2/sites-available/default-ssl':
      owner   => 'root',
      group   => 'root',
      mode    => '0775',
      content => template('php/apache2/sites-available/default-ssl.erb'),
      require => Exec['clean sites'];

    '/etc/apache2/sites-enabled/default':
      ensure  => 'link',
      target  => '/etc/apache2/sites-available/default',
      require => File['/etc/apache2/sites-available/default'];

    '/etc/apache2/sites-enabled/default-ssl':
      ensure  => 'link',
      target  => '/etc/apache2/sites-available/default-ssl',
      require => File['/etc/apache2/sites-available/default-ssl'];	
  }

  php::importssl {'import ssl':
     ssl_certificate_file => $ssl_certificate_file,
     ssl_key_file         => $ssl_key_file,
     require               => File['/etc/apache2/apache2.conf']
  }

  exec {
    'enable ssl module':
      path    => ['/bin', '/usr/bin', '/usr/sbin/'],
      command => 'a2enmod ssl',
      require => [ File['/etc/apache2/apache2.conf'],
                   Php::Importssl['import ssl']
                 ];

    'apache2 restart':
      path    => ['/bin', '/usr/bin', '/usr/sbin/'],
      command => "/etc/init.d/apache2 restart",
      require => [ Exec["enable ssl module"],
                   File['/etc/apache2/sites-enabled/default'], 
                   File['/etc/apache2/sites-enabled/default-ssl']
                 ];
  }

  #install stratos_base before java before php before agent
  Class['stratos_base'] -> Class['python_agent'] -> Class['php']
}
