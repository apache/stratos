stage { 'configure': require => Stage['main'] }
stage { 'deploy': require => Stage['configure'] }

node basenode {
#        $package_repo 		= "http://puppet-stratos-packs.s3-website-us-east-1.amazonaws.com/stratos-1.6"
        $depsync_svn_repo 	= "https://sn.s2.wso2.com/wso2/repo/slive/"
	$local_package_dir	= "/mnt/packs/"
	$deploy_new_packs	= "true"
}

node confignode inherits basenode  {
	## Service subdomains
	$stratos_domain 	= "S2DOMAIN"
	$as_subdomain 		= "appserver"
	$mb_subdomain 		= "messaging"
	$governance_subdomain	= "governance"
        $management_subdomain   = "mgt"
        $bam_subdomain          = "monitor"
        $bps_subdomain          = "bps"
        $brs_subdomain          = "rule"
        $cep_subdomain          = "cep"
        $dss_subdomain          = "data"
        $esb_subdomain          = "esb"
        $gs_subdomain           = "gadget"
        $cg_subdomain           = "cloudgateway"
        $ts_subdomain           = "task"
        $is_subdomain           = "identity"
        $ms_subdomain           = "mashup"
        $ss_subdomain           = "storage"
        $am_subdomain           = "api"


	## ADC
	$adc_tribes_port		= "5001"
	$adc_host		= "sc.S2DOMAIN"
	$adc_port		= "9445"


	## Server details for billing
	$time_zone		= "GMT-8:00"

	## Userstore MySQL server configuration details
        $mysql_server_1         = "mysql1.s2.wso2.com"
        $mysql_server_2         = "mysql1.s2.wso2.com"
	$mysql_userstore	= "sc.S2DOMAIN"
        $mysql_port             = "3306"
        $max_connections        = "100000"
        $max_active             = "150"
        $max_wait               = "360000"
	## User store config Database detilas
        $registry_user          = "registry"
        $registry_password      = "registry123"
        $registry_database      = "governance"

	$rss_database           = "rss_db"
        $rss_user               = "rss_db"
        $rss_password           = "rss123"
        $rss_instance_user      = "wso2admin"
        $rss_instance_password  = "wso2admin123"


	$userstore_user         = "root"
        $userstore_password     = "g"
        $userstore_database     = "userstore"

	$billing_user           = "billing"
        $billing_password       = "billing123"
        $billing_database       = "billing"
        $billing_datasource     = "WSO2BillingDS"

        ## Cassandra details
        $css0_subdomain         = "node0.cassandra"
        $css1_subdomain         = "node1.cassandra"
        $css2_subdomain         = "node2.cassandra"
        $css_cluster_name       = "Stratos Dev Setup"
        $css_port               = "9160"
        $cassandra_username     = "admin"
        $cassandra_password     = "admin"
        $hdfs_url               = "hadoop0"
        $hdfs_port              = "9000"
        $hdfs_job_tracker_port  = "9001"

        $super_admin_email      = "sanjaya@wso2.com"
        $notification_email     = "damitha@wso2.com"
        $finance_email          = "amilam@wso2.com"
	$stratos_admin_user     = "stratos"
        $stratos_admin_password = "stratos123"

        ## LOGEVENT configurations
        $receiver_url           = "receiver.S2DOMAIN"
        $receiver_port          = "7614"
        $receiver_secure_port   = "7714"
        $receiver_username      = "admin"
        $receiver_password      = "admin"

	## Deployment synchronizer
	$repository_type 	= "svn"
	$svn_user 		= "wso2"
	$svn_password 		= "wso2123"	


	## ELB 
	$elb_http1_port         = "8280"    
	$elb_https1_port        = "8243"    

	$elb_http2_port         = "8290"    
	$elb_https2_port        = "8253"  

}

### PUPPET-DEV STRATOS NODES IN LK VMs ####

node 'puppet.novalocal' inherits confignode {
	$server_ip 	= $ipaddress
	
	## Automatic failover
#        $virtual_ip     = "192.168.4.250"
#        $interface      = "eth0"
#        $check_interval = "2"
#        $priority       = "100"
#        $state          = "MASTER"
	
	include system_config
        

#	class {"stratos::elb":
#                services           =>  ["identity,*,mgt",
#                                        "governance,*,mgt"],
#                version            => "2.0.3",
#                maintenance_mode   => "true",
#                auto_scaler        => "false",
#                auto_failover      => "false",
#		owner		   => "root",
#		group		   => "root",
#		target             => "/mnt/${server_ip}",
#                stage              => "deploy",
#        }

class {"stratos::mb":
                version            => "2.0.1",
                offset             => 1,
                css_port            => 9161,
                maintenance_mode   => "true",
                owner              => "ubuntu",
                group              => "ubuntu",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
        }

class {"stratos::cc":
                version            => "1.0.0",
                offset             => 4,
                mb_host		   => "localhost",
                mb_port		   => "5673",
                maintenance_mode   => "true",
                owner              => "ubuntu",
                group              => "ubuntu",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
        }


}

node 'php.s2.wso2.com'{

	class {"php_cartridge":
			syslog            => "syslog:local2",
			docroot		  => "/var/www/www",
			samlalias	  => "/var/www/simplesamlphp/www",
	}

}

node 'php.stratoslive.wso2.com'{

	class {"php_cartridge":
			syslog            => "syslog:local2",
			docroot		  => "/var/www/www",
			samlalias	  => "/var/www/simplesamlphp/www",
	}

}
node 'mgt.appserver.s2.wso2.com' inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include system_config

	class {"stratos::appserver":

        
	        version            => "5.0.2",
                offset             => 1,
                tribes_port        => 4100,
                config_db          => "appserver_config",
                maintenance_mode   => "false",
                depsync            => "true",
                sub_cluster_domain => "mgt",
                owner              => "kurumba",
                group              => "kurumba",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                repository_type    => "git",
		cartridge_type	   => "appserver",
		generic		   => 0,
        }
}

node 'default.appserver.s2.wso2.com' inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include system_config

	class {"stratos::appserver":
        
	        version            => "5.0.2",
                offset             => 1,
                tribes_port        => 4100,
                config_db          => "appserver_config",
                maintenance_mode   => "false",
                depsync            => "true",
                sub_cluster_domain => "mgt",
                owner              => "kurumba",
                group              => "kurumba",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                repository_type    => "git",
		cartridge_type	   => "appserver",
		generic		   => 1,

        }

}

node /[0-9]{1,12}.default.appserver/ inherits confignode{
        $server_ip      = $ec2_local_ipv4

        include system_config

        class {"stratos::appserver":

                version            => "5.0.2",
                offset             => 1,
                tribes_port        => 4100,
                config_db          => "appserver_config",
                maintenance_mode   => "false",
                depsync            => "true",
                sub_cluster_domain => "mgt",
                owner              => "kurumba",
                group              => "kurumba",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                repository_type    => "git",
                cartridge_type     => "${s2_instance_data_service}",
                generic            => 1,
                multitenant        => "${s2_instance_data_multitenant}",

        }

}

node 'worker.appserver.s2.wso2.com' inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include system_config

	class {"stratos::appserver":

        
	        version            => "5.0.2",
                offset             => 0,
                tribes_port        => 4100,
                config_db          => "appserver_config",
                maintenance_mode   => "false",
                depsync            => "true",
                sub_cluster_domain => "worker",
                owner              => "kurumba",
                group              => "kurumba",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                repository_type    => "git",
		cartridge_type	   => "appserver",
		generic		   => 0,

        }

}

node 'esb.s2.wso2.com' inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include system_config

	class {"stratos::esb":
                version            => "4.6.0",
                offset             => 2,
                tribes_port        => 4200,
                config_db          => "esb_config",
                maintenance_mode   => "true",
                depsync            => "true",
                sub_cluster_domain => "worker",
		owner		   => "kurumba",
		group		   => "kurumba",
                target             => "/mnt/${server_ip}",
                stage              => "deploy",
        }
}

node 'default.bps.s2.wso2.com' inherits confignode{
        $server_ip      = $ec2_local_ipv4

	include system_config

	class {"stratos::bps":
                version            => "3.0.1",
                offset             => 0,
                tribes_port        => 4000,
                config_db          => "bps_config",
                maintenance_mode   => "true",
                depsync            => "true",
                owner              => "kurumba",
                group              => "kurumba",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                target             => "/mnt/${server_ip}",
#               sub_cluster_domain => "worker",
                cartridge_type     => "bps",
	}
}

node /[0-9]{1,12}.default.bps/ inherits confignode{
        $server_ip      = $ec2_local_ipv4

        include system_config

        class {"stratos::bps":
                version            => "3.0.1",
                offset             => 0,
                tribes_port        => 4000,
                config_db          => "bps_config",
                maintenance_mode   => "true",
                depsync            => "true",
                owner              => "kurumba",
                group              => "kurumba",
                adc_host           => "sc.S2DOMAIN",
                adc_port           => 9445,
                target             => "/mnt/${server_ip}",
                cartridge_type     => "${s2_instance_data_service}",
		generic            => 1,
                multitenant        => "${s2_instance_data_multitenant}",
        }
}

## ESB

node 'mgt.esb.s2.wso2.com' inherits confignode{
                $server_ip      = $ipaddress

                include system_config

                class {"stratos::esbserver":

                                version            => "4.6.0",
                                offset             => 0,
                                tribes_port        => 4100,
                                config_db          => "esb_config",
                                maintenance_mode   => "false",
                                depsync            => "true",
                                sub_cluster_domain => "mgt",
                                owner              => "ubuntu",
                                group              => "ubuntu",
                                target             => "/mnt/${server_ip}",
                                stage              => "deploy",
                                repository_type    => "git",
                                cartridge_type     => "esb",
                                generic            => 1,                                                                
                }

}


node 'worker.esb.s2.wso2.com' inherits confignode{
                $server_ip      = $ipaddress

                include system_config

                class {"stratos::esbserver":
                                version            => "4.6.0",
                                tribes_port        => 4100,
                                config_db          => "esb_config",
                                offset             => 0,
                                maintenance_mode   => "false",
                                depsync            => "true",
                                sub_cluster_domain => "worker",
                                owner              => "ubuntu",
                                group              => "ubuntu",
                                target             => "/mnt/${server_ip}",
                                stage              => "deploy",
                                repository_type    => "git",
                                cartridge_type     => "esb",
                                generic            => 1,
                }

}

node 'default.esb.s2.wso2.com' inherits confignode{
                $server_ip      = $ipaddress

                include system_config

                class {"stratos::esbserver":

                                version            => "4.6.0",
                                offset             => 0,
                                tribes_port        => 4100,
                                config_db          => "esb_config",
                                maintenance_mode   => "false",
                                depsync            => "true",
                                sub_cluster_domain => "mgt",
                                owner              => "ubuntu",
                                group              => "ubuntu",
                                target             => "/mnt/${server_ip}",
                                stage              => "deploy",
                                repository_type    => "git",
                                cartridge_type     => "esb",
                                generic            => 1,
                }

}

node /[0-9]{1,12}.default.esb/ inherits confignode{
                $server_ip      = $ipaddress

                include system_config

                class {"stratos::esbserver":

                                version            => "4.6.0",
                                offset             => 0,
                                tribes_port        => 4100,
                                config_db          => "esb_config",
                                maintenance_mode   => "false",
                                depsync            => "true",
                                sub_cluster_domain => "mgt",
                                owner              => "ubuntu",
                                group              => "ubuntu",
                                target             => "/mnt/${server_ip}",
                                stage              => "deploy",
                                repository_type    => "git",
                                cartridge_type     => "${s2_instance_data_service}",
                                generic            => 1,
                                multitenant        => "${s2_instance_data_multitenant}",
                }

}


