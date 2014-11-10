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

class wordpress (
  $httpd_user  = 'apache',
  $httpd_group = 'apache',
  $docroot     = '/var/www/html/',
  $wp_password = 'wordpress'
) { 

  class {'python_agent':}

  $packages = [
    'httpd',
    'openssl.x86_64',
    'mod_ssl.x86_64',
    'php',
    'php-adodb.noarch',
    'php-dba.x86_64',
    'php-gd.x86_64',
    'php-imap.x86_64',
    'php-ldap.x86_64',
    'php-mcrypt.x86_64',
    'php-mysql.x86_64',
    'php-pear.noarch',
    'php-xml.x86_64',
    'php-xmlrpc.x86_64',
    'php.x86_64',
    'git-all.noarch',
    'mysql-server',
  ]

  exec {
    'Download wordpress':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd     => '/tmp',
      unless  => 'test -f wordpress.tar.gz',
      command => "wget -q ${package_repo}/wordpress.tar.gz",
      require => Package['httpd'];

    'Exctract wordpress to docroot':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd     => $docroot,
      command => "tar xvfz /tmp/wordpress.tar.gz",
      require => Exec['Download wordpress'];

    'Set permission':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd     => $docroot,
      command => "chown -R ${httpd_user}:${httpd_group} ${docroot}/wordpress; chmod -R 755 ${docroot}/wordpress",
      require => Exec['Exctract wordpress to docroot'];
  }

  file {
    '/etc/httpd/conf/httpd.conf':
      owner   => 'root',
      group   => 'root',
      mode    => '0775',
      notify  => Service['httpd'],
      content => template('wordpress/httpd/httpd.conf.erb'),
      require => Package['httpd'];
  }

  exec { 
    'Create wordpress database':
      path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      command => "mysql -uroot -p${root_password} -Bse \"CREATE DATABASE IF NOT EXISTS wordpress\"";

    'Grant permission':
      path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      command => "mysql -uroot -p${root_password} -Bse \"GRANT ALL on wordpress.* to wordpress@localhost identified by 'wordpress'\"",
      require => Exec['Create wordpress database'];
  }
 
  # install stratos_base before java before mysql before wordpress before agent
  Class['stratos_base'] -> Class['python_agent'] -> Class['mysql'] -> Class['wordpress']
}
