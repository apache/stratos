node 'base' {

  #essential variables
  $package_repo         = 'http://10.4.128.7'
  $local_package_dir    = '/mnt/packs'
  $mb_ip                = '54.255.43.95'
  $mb_port              = '5677'
  $mb_type		= 'activemq' #in wso2 mb case, value should be 'wso2mb'
  $cep_ip               = '54.255.43.95'
  $cep_port             = '7615'
  $truststore_password  = 'wso2carbon'
  $java_distribution	= 'jdk-7u7-linux-x64.tar.gz'
  $java_name		= 'jdk1.7.0_07'
  $member_type_ip       = 'private'
  $lb_httpPort          = '80'
  $lb_httpsPort         = '443'
  $tomcat_version       = '7.0.52'

}

# php cartridge node
node /php/ inherits base {
  $docroot = "/var/www/"
  $syslog="/var/log/apache2/error.log"
  $samlalias="/var/www/"
  require java
  class {'agent':}
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
  Class['tomcat'] ~> Class['agent']
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
  class {'agent':}
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

# stratos components related nodes
# not supported in alpha version.
node 'autoscaler.wso2.com' inherits base {
  require java
  class {'autoscaler': maintenance_mode => 'norestart',}
}

node 'cc.wso2.com' inherits base {
  require java
  class {'cc': maintenance_mode   => 'norestart',}
}

node 'cep.wso2.com' inherits base {
  require java
  class {'cep': maintenance_mode   => 'norestart',}
}


node 'mb.wso2.com' inherits base {
  require java
  class {'messagebroker': maintenance_mode   => 'norestart',}
}

node 'sc.wso2.com' inherits base {
  require java
  class {'manager': maintenance_mode   => 'norestart',}
}


