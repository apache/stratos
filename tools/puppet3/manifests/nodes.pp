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

node 'base' {

  #essential variables
  $package_repo         = 'http://10.4.128.7'
  $local_package_dir    = '/mnt/packs'
  $mb_ip                = 'MB_IP'
  $mb_port              = 'MB_PORT'
  $mb_type		= 'activemq' #in wso2 mb case, value should be 'wso2mb'
  $cep_ip               = '127.0.0.1'
  $cep_port             = '7611'
  $truststore_password  = 'wso2carbon'
  $java_distribution	= 'jdk-7u51-linux-x64.tar.gz'
  $java_name		= 'jdk1.7.0_51'
  $java_home            = '/opt/java'
  $member_type_ip       = 'private'
  $lb_httpPort          = '80'
  $lb_httpsPort         = '443'
  $tomcat_version       = '7.0.52'
  $enable_log_publisher = 'false'
  $bam_ip		= '127.0.0.1'
  $bam_port		= '7611'
  $bam_secure_port	= '7711'
  $bam_username		= 'admin'
  $bam_password		= 'admin'
  $ssl_enabled          = 'false'
  $ssl_certificate_file = ''
  $ssl_key_file         = ''
  $ssl_ca_cert_file     = ''

  $extension_instance_started             = 'instance-started.sh'
  $extension_start_servers                = 'start-servers.sh'
  $extension_instance_activated           = 'instance-activated.sh'
  $extension_artifacts_updated            = 'artifacts-updated.sh'
  $extension_clean                        = 'clean.sh'
  $extension_mount_volumes                = 'mount-volumes.sh'
  $extension_member_started               = 'member-started.sh'
  $extension_member_activated             = 'member-activated.sh'
  $extension_member_terminated            = 'member-terminated.sh'
  $extension_member_suspended             = 'member-suspended.sh'
  $extension_complete_topology            = 'complete-topology.sh'
  $extension_complete_tenant              = 'complete-tenant.sh'
  $extension_subscription_domain_added    = 'subscription-domain-added.sh'
  $extension_subscription_domain_removed  = 'subscription-domain-removed.sh'
  $extension_artifacts_copy               = 'artifacts-copy.sh'
  $extension_tenant_subscribed            = 'tenant-subscribed.sh'
  $extension_tenant_unsubscribed          = 'tenant-unsubscribed.sh'
  $agent_log_level = "INFO"
  $extensions_dir = '${script_path}/../extensions'
}

# Jboss cartridge node
node /jboss/ inherits base {
  $product = 'jboss-as'
  $version = '7.1.1.Final'
  $docroot = "/mnt/${product}-${version}/standalone/deployments/"
  $jboss_user    = 'jbossas1'
  $jboss_group   = 'jboss'
  require java
  class {'jboss': 
     user       => $jboss_user,
     group      => $jboss_group,
     java_home  => $java_home,
     version    => $version,
     java_opts  => "-Xms512m -Xmx3000m",
     extra_jars => ['mysql-connector-java-5.1.29-bin.jar']
  }

  class {'agent':
     type => $product
  }

  Class['jboss'] -> Class['agent']
}

# php cartridge node
node /php/ inherits base {
  $docroot = "/var/www/"
  $syslog="/var/log/apache2/error.log"
  $samlalias="/var/www/"
  require java
  class {'agent':
    type => 'php',
  }
  class {'php':}
  
  #install php before agent
  Class['php'] ~> Class['agent']
}

# loadbalancer cartridge node
node /lb/ inherits base {
  require java
  class {'agent':}
  class {'lb': maintenance_mode   => 'norestart',}
}

# tomcat cartridge node
node /tomcat/ inherits base {
  $docroot = "/mnt/apache-tomcat-${tomcat_version}/webapps/"
  $samlalias="/mnt/apache-tomcat-${tomcat_version}/webapps/"

  require java
  class {'agent':}
  class {'tomcat':}

  #install tomcat befor agent
  #Class['tomcat'] ~> Class['agent']
}

# mysql cartridge node
node /mysql/ inherits base {
  require java
  class {'agent':
    type => 'mysql',
  }
  class {'mysql':}
}

# nodejs cartridge node
node /nodejs/ inherits base {
  require java
  class {'agent':
    type => 'nodejs',
  }
  class {'nodejs':}

  #install agent before nodejs
  Class['nodejs'] ~> Class['agent']
}

# haproxy extension loadbalancer cartridge node
node /haproxy/ inherits base {
  require java
  class {'haproxy':}
  class {'agent':}
}

# ruby cartridge node
node /ruby/ inherits base {
  require java
  class {'agent':
  }
  class {'ruby':}
#  Class['ruby'] ~> Class['agent']
}

#wordpress cartridge node
node /wordpress/ inherits base {
  class {'agent':}
  class {'wordpress':}
  class {'mysql':}

}

# default (base) cartridge node
node /default/ inherits base {
  require java
  class {'agent':}
}
