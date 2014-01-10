## --------------------------------------------------------------
##
## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.
##
## --------------------------------------------------------------

stage { 'configure': require => Stage['main'] }
stage { 'deploy': require => Stage['configure'] }

node basenode {
	$local_package_dir	= "/mnt/packs/"
	$deploy_new_packs	= "true"
}

node confignode inherits basenode  {
	## Service subdomains
	$stratos_domain 	= "stratos.org"

	## Server details for billing
	$time_zone		= "GMT-8:00"

}

node 'puppet.novalocal' inherits confignode {
	$server_ip 	= $ipaddress
	
	include system_config


}


node /[0-9]{1,12}.default.php/ {
       
	 include java
 
	class {"php_cartridge":
                        syslog 		           => "syslog:local2",
                        docroot         	   => "/var/www/www",
                        samlalias        	   => "/var/www/simplesamlphp/www",
                        serverport        	   => "443",
                        mb_ip       		   => "54.251.211.89",
                        mb_port       		   => "5677",
                        cep_ip        		   => "54.251.211.89",
                        cep_port      		   => "7615",
			cert_truststore  	   => "client-truststore.jks",
                        truststore_password  	   => "wso2carbon",

        }
}

node /[0-9]{1,12}.default.lb/ inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include java

        class {"stratos::lb":
                version            => "4.0.0-SNAPSHOT",
                offset             => 0,
                tribes_port        => 4100,
                maintenance_mode   => "false",
                owner              => "root",
                group              => "root",
                stage              => "deploy",
                adc_host           => "sc.stratos.org",
                adc_port           => 9445,
                cartridge_type     => "load-balancer",
                generic            => 1,
                mb_ip             => "54.251.196.18",
                mb_port           => "5677",
                cep_ip            => "54.251.196.18",
                cep_port          => "7615",
                java_truststore   => "client-truststore.jks",
                java_truststore_password => "wso2carbon",


        }   

}

