class stratos::lb ( $version, 
		   $offset=0, 
		   $tribes_port=4000, 
		   $config_db=governance, 
		   $maintenance_mode=true, 
		   $owner=root,
		   $group=root,
		   $adc_host="lb.strato.org",
                   $adc_port=9445,
		   $target="/opt",
		   $cartridge_type,
		   $generic, 
	           $mb_ip,
                   $mb_port,
                   $cep_ip,
                   $cep_port,
                   $java_truststore,
                   $java_truststore_password) {
	
	
	$deployment_code	= "load-balancer"

	$stratos_version 	= $version
	$service_code 		= "load-balancer"
	$carbon_home		= "${target}/apache-stratos-load-balancer-${stratos_version}"
	$mgt_port		= 8280 + $offset


        $common_templates      =  [
				#	"conf/datasources/master-datasources.xml",
				#	"conf/etc/logging-config.xml",
				#	"conf/log4j.properties"
				]


	tag ($service_code)

       define push_templates ( $directory, $target ) {
       
             #  file { "${target}/repository/${name}":
             #          owner   => $owner,
             #          group   => $group,
             #          mode    => 755,
             #          content => template("${directory}/${name}.erb"),
             #          ensure  => present,
             #  }
       }

	clean { $deployment_code:
		mode		=> $maintenance_mode,
                target          => $carbon_home,
	}

	initialize { $deployment_code:
		version         => $stratos_version,
		mode		=> $maintenance_mode,
		service		=> $service_code,
		local_dir       => $local_package_dir,
		owner		=> $owner,
		target   	=> $target,
		require		=> Stratos::Clean[$deployment_code],
	}

	deploy { $deployment_code:
		service		=> $service_code,	
		security	=> "true",
		owner		=> $owner,
		group		=> $group,
		target		=> $carbon_home,
		require		=> Stratos::Initialize[$deployment_code],
	}


	
	push_templates { 
		$service_templates: 
		target		=> $carbon_home,
		directory 	=> $service_code,
		require 	=> Stratos::Deploy[$deployment_code];

		$common_templates:
		target          => $carbon_home,
                directory       => "commons",
		require 	=> Stratos::Deploy[$deployment_code],
	}

	start { $deployment_code:
		owner		=> $owner,
                target          => $carbon_home,
		require		=> [ Stratos::Initialize[$deployment_code],
				     Push_templates[$service_templates],
				     Push_templates[$common_templates], 
				   ],
	}

        file {  "/opt/apache-stratos-cartridge-agent":
                ensure  => directory 
        }


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
                 content => ",MB_IP=${mb_ip},MB_PORT=${mb_port},CEP_IP=${cep_ip},CEP_PORT=${cep_port},CERT_TRUSTSTORE=${java_truststore},TRUSTSTORE_PASSWORD=${java_truststore_password}",
	}

        exec {"run_agent_script-$deployment_code":
                command => "/opt/apache-stratos-cartridge-agent/cartridge-agent.sh",
                require => File["/tmp/puppet-payload",
		"/opt/apache-stratos-cartridge-agent/apache-stratos-cartridge-agent-4.0.0-SNAPSHOT-bin.zip",
		"/opt/apache-stratos-cartridge-agent/cartridge-agent.sh",
		"/opt/apache-stratos-cartridge-agent/get-launch-params.rb"];
        }

}

