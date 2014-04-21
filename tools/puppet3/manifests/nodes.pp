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
  $mb_ip                = '127.0.0.1'
  $mb_port              = '61616'
  $mb_type		= 'activemq' #in wso2 mb case, value should be 'wso2mb'
  $cep_ip               = '127.0.0.1'
  $cep_port             = '7611'
  $truststore_password  = 'wso2carbon'
  $java_distribution	= 'jdk-7u51-linux-x64.tar.gz'
  $java_name		= 'jdk1.7.0_51'
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
