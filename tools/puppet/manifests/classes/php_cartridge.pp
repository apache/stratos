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


class php_cartridge (syslog,docroot,samlalias,serverport,mb_ip,mb_port,cep_ip,cep_port,cert_truststore,truststore_password){
	
	$packages = ["nano","zip","build-essential","mysql-client","apache2","php5","php5-cli","libapache2-mod-php5","php5-gd","php5-mysql","php-db","php-pear","php5-curl","curl","wget","php5-ldap","php5-adodb","mailutils","php5-imap","php5-sqlite","php5-xmlrpc","php5-xsl","openssl","ssl-cert","ldap-utils","php5-mcrypt","mcrypt","ufw","fail2ban","git","libboost-all-dev","ruby"]

        file { "/etc/apt/apt.conf.d/90forceyes":
                ensure  => present,
                source  => "puppet:///apt/90forceyes";
        }

#fix for the apt get failure: multiple paths to 'path'. on a failure, logs are enabled to get an idea of what is wrong.

        exec { "update-apt":
                path    => ['/bin', '/usr/bin', '/sbin', '/usr/sbin/'],
                command => "apt-get update > /dev/null 2>&1 ",
                require => File["/etc/apt/apt.conf.d/90forceyes"],
		logoutput => on_failure,
        }

        package { $packages:
                provider        => apt,
                ensure          => installed,
                require         => Exec["update-apt"],
        }	
	
	file { 
		"/etc/apache2/apache2.conf":
        	owner   => root,
        	group   => root,
        	mode    => 775,
        	content => template("php_cartridge/etc/apache2/apache2.conf.erb"),
                require         => Package['apache2'];
    	
	        "/etc/apache2/sites-available/default":
        	owner   => root,
        	group   => root,
        	mode    => 775,
        	content => template("php_cartridge/etc/apache2/sites-available/default.erb"),
                require         => File["/etc/apache2/apache2.conf"];
	
		"/etc/apache2/sites-available/default-ssl":
        	owner   => root,
        	group   => root,
        	mode    => 775,
        	content => template("php_cartridge/etc/apache2/sites-available/default-ssl.erb"),
                require         => File["/etc/apache2/sites-available/default"];
    	}
	
	exec { "enable ssl module":
                path    => ['/bin', '/usr/bin','/usr/sbin/'],
                command => "a2enmod ssl",
                require         => File["/etc/apache2/sites-available/default-ssl"];
	
		"enable ssl":
                path    => ['/bin', '/usr/bin','/usr/sbin/'],
                command => "a2enmod ssl",
                require         => Exec["enable ssl module"];
		
		"apache2 restart":
                path    => ['/bin', '/usr/bin','/usr/sbin/'],
                command => "/etc/init.d/apache2 restart",
                require         => Exec["enable ssl"];
	}


	# -----  port check failed for port 443 -- recheck
	#exec {"wait_for_server-$deployment_code":
        #    command => "/usr/bin/wget --spider --tries 100 --retry-connrefused --no-check-certificate https://$ipaddress:$serverport",
        #    require => Exec["apache2 restart"];
        # }


	# Copy and configure agent packs
        #file {  "/opt/apache-stratos-cartridge-agent/apache-stratos-event-publisher-4.0.0-SNAPSHOT-bin.zip":
        #	source          => "puppet:///commons/apache-stratos-event-publisher-4.0.0-SNAPSHOT-bin.zip",
        #        ensure          => present,
	#} 	

        #file {  "/opt/apache-stratos-cartridge-agent/apache-stratos-event-subscriber-4.0.0-SNAPSHOT-bin.zip":
        #        source          => "puppet:///commons/apache-stratos-event-subscriber-4.0.0-SNAPSHOT-bin.zip",
        #        ensure          => present,
        #}


        #file {  "/opt/apache-stratos-cartridge-agent/apache-stratos-health-publisher-4.0.0-SNAPSHOT-bin.zip":
        #        source          => "puppet:///commons/apache-stratos-health-publisher-4.0.0-SNAPSHOT-bin.zip",
        #        ensure          => present,
        #}

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


        file {  "/opt/apache-stratos-cartridge-agent/healthcheck.sh":
                source          => "puppet:///commons/healthcheck.sh",
                ensure          => present,
        }


	file {"/tmp/puppet-payload":
		 ensure  => present,
		 content => ",MB_IP=${mb_ip},MB_PORT=${mb_port},CEP_IP=${cep_ip},CEP_PORT=${cep_port},CERT_TRUSTSTORE=${cert_truststore},TRUSTSTORE_PASSWORD=${truststore_password},APP_PATH=${docroot}",
                 require => Exec["apache2 restart"];
	}

        exec {"run_agent_script-$deployment_code":
                command => "/opt/apache-stratos-cartridge-agent/cartridge-agent.sh",
                require => File["/tmp/puppet-payload"];
        }
	



}
