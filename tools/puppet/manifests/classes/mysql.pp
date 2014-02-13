# --------------------------------------------------------------
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
# --------------------------------------------------------------


class mysql (mb_ip,mb_port,cep_ip,cep_port,java_truststore,java_truststore_password,enable_data_publisher,monitoring_server_ip,monitoring_server_port,monitoring_server_secure_port,monitoring_server_admin_username,monitoring_server_admin_password) {

  if $stratos_mysql_password {
    $root_password = $stratos_mysql_password
  }
  else {
    $root_password = 'root'
  }

  file { "/etc/apt/apt.conf.d/90forceyes":
                ensure  => present,
                source  => "puppet:///apt/90forceyes";
   }

   exec { "update-apt":
            path    => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
            command => "apt-get update > /dev/null 2>&1 ",
            require => File["/etc/apt/apt.conf.d/90forceyes"],
            logoutput => on_failure,
   }

  package { ['mysql-server','phpmyadmin','apache2']:
    ensure => installed,
    require => Exec["update-apt"],
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
#    require => Service['mysql'];
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

#  file { '/etc/httpd/conf.d/phpMyAdmin.conf':
#    ensure  => present,
#    content => template('mysql/phpMyAdmin.conf.erb'),
#    notify  => Service['httpd'],
#    require => [
#      Package['phpmyadmin'],
#      Package['apache2'],
#    ];
#  }


        file {  "/opt/apache-stratos-cartridge-agent/apache-stratos-cartridge-agent-4.0.0-SNAPSHOT-bin.zip":
                source          => "puppet:///commons/apache-stratos-cartridge-agent-4.0.0-SNAPSHOT-bin.zip",
                ensure          => present,
        }

        file {  "/opt/apache-stratos-cartridge-agent/cartridge-agent.sh":
                source          => "puppet:///commons/cartridge-agent.sh",
                ensure          => present,
        }


        file {  "/opt/apache-stratos-cartridge-agent/get-launch-params.rb":
                source          => "puppet:///commons/get-launch-params.rb",
                ensure          => present,
        }


        file {"/tmp/puppet-payload":
                 ensure  => present,
                 content => ",MB_IP=${mb_ip},MB_PORT=${mb_port},CEP_IP=${cep_ip},CEP_PORT=${cep_port},CERT_TRUSTSTORE=${java_truststore},TRUSTSTORE_PASSWORD=${java_truststore_password},APP_PATH=null,ENABLE_DATA_PUBLISHER=${enable_data_publisher},MONITORING_SERVER_IP=${monitoring_server_ip},MONITORING_SERVER_PORT=${monitoring_server_port},MONITORING_SERVER_SECURE_PORT=${monitoring_server_secure_port},MONITORING_SERVER_ADMIN_USERNAME=${monitoring_server_admin_username},MONITORING_SERVER_ADMIN_PASSWORD=${monitoring_server_admin_password}",
                 require => Service['apache2'];
        }

        exec {"run_agent_script-$deployment_code":
                command => "/opt/apache-stratos-cartridge-agent/cartridge-agent.sh",
                require => File["/tmp/puppet-payload"];
        }


}
