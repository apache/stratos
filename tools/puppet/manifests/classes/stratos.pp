# Parent of a startos service deployment

class stratos {

## Cleans the previous deployment. If the maintenance mode is set to true, this will only kill the running service.
	
	define clean ( $mode, $target ) {	

		exec { "remove_${name}_poop":
                	path            => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/",
	                onlyif          => "test -d ${target}/repository",
        	        command         => $mode ? {
                	                        "true"  => "kill -9 `cat ${target}/stratos.pid` ; /bin/echo Killed",
                        	                "false" => "kill -9 `cat ${target}/stratos.pid` ; rm -rf ${target}",
                        	                "fresh" => "kill -9 `cat ${target}/stratos.pid` ; rm -rf ${target} ; rm -f ${local_dir}/apache-stratos-${service}-${version}.zip",
                                	   },
		}
	}

## Initializing the deployment by placing a customized script in /opt/bin which will download and extract the pack.

	define initialize (  $version, $service, $local_dir, $target, $mode, $owner ) {

		file {  "${local_dir}/apache-stratos-${service}-${version}.zip":
			source		=> "puppet:///commons/apache-stratos-${service}-${version}.zip",
			ensure		=> present,
			require		=> [ Exec["creating_target_for_${name}"],
					     Exec["creating_local_package_repo_for_${name}"] ];
		}

		exec {  "creating_target_for_${name}":
			path    	=> ["/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],
			command		=> "mkdir -p ${target}";

			"creating_local_package_repo_for_${name}":
			path		=> "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/",
			unless		=> "test -d ${local_dir}",
			command		=> "mkdir -p ${local_dir}";
		
		
			"extracting_apache-stratos-${service}-${version}.zip_for_${name}":
			path    	=> ["/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],
			cwd		=> $target,
			unless		=> "test -d ${target}/apache-stratos-${service}-${version}/repository",
			command 	=> "unzip ${local_dir}/apache-stratos-${service}-${version}.zip",
			logoutput       => "on_failure",
			creates		=> "${target}/apache-stratos-${service}-${version}/repository",
                        timeout 	=> 0,
                        require 	=> File["${local_dir}/apache-stratos-${service}-${version}.zip"];

			"setting_permission_for_${name}":
			path            => ["/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],
                        cwd             => $target,
			command         => "chown -R ${owner}:${owner} ${target}/apache-stratos-${service}-${version} ;
					    chmod -R 755 ${target}/apache-stratos-${service}-${version}",
			logoutput       => "on_failure",
			timeout         => 0,
                        require 	=> Exec["extracting_apache-stratos-${service}-${version}.zip_for_${name}"];


                        "update_loadbalancer_conf":
                        path            => ["/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"],
                        cwd             => $target,
                        command         => "sed -i 's/mb-ip: .*;/mb-ip: $mb_ip;/g;s/mb-port: .*;/mb-port: $mb_port;/g;s/cep-ip: .*;/cep-ip: $cep_ip;/g;s/cep-port: .*;/cep-port: $cep_port;/g' ${target}/apache-stratos-${service}-${version}/repository/conf/loadbalancer.conf",
                        logoutput       => "on_failure",
                        timeout         => 0,
                        require         => Exec["setting_permission_for_${name}"];
			

		}

	}


## Executes the deployment by pushing all necessary configurations and patches

	define deploy ( $service, $security, $target, $owner, $group ) {
		

	}

## Starts the service once the deployment is successful.

	define start ( $target, $owner ) {
		exec { "strating_${name}":
			user		=> $owner,
			path            => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/",
			environment     => 'JAVA_HOME=/opt/java',
	                command         => "${target}/bin/stratos.sh > /var/log/lb_start.log 2>&1 &",
        	        creates         => "${target}/repository/wso2server.log",
		}
	}
}
