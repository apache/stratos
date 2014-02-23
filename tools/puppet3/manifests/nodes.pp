node 'base' {

  #essential variables
  $package_repo         = 'http://10.4.128.7'
  $local_package_dir    = '/mnt/packs'
  $mb_ip                = '10.4.128.12'
  $mb_port              = '5672'
  $cep_ip               = '10.4.128.10'
  $cep_port             = '7611'
  $truststore_password    = 'wso2carbon'

  #following variables required only if you want to install stratos using puppet.
  #not supported in alpha version
  # Service subdomains
  #$domain               = 'wso2.com'
  #$as_subdomain         = 'autoscaler'
  #$management_subdomain = 'management'

  #$admin_username       = 'admin'
  #$admin_password       = 'admin123'

  #$puppet_ip            = '10.4.128.7'
  


  #$cc_ip                = '10.4.128.9'
  #$cc_port              = '9443'

  #$sc_ip                = '10.4.128.13'
  #$sc_port              = '9443'

  #$as_ip                = '10.4.128.8'
  #$as_port              = '9443'

  #$git_hostname        = 'git.wso2.com'
  #$git_ip              = '10.4.128.13'

  #$mysql_server        = '10.4.128.13'
  #$mysql_user          = 'root'
  #$mysql_password      = 'root'

  #$bam_ip              = '10.4.128.15'
  #$bam_port            = '7611'
  
  #$internal_repo_user     = 'admin'
  #$internal_repo_password = 'admin'

}

# php cartridge node
node /php/ inherits base {
  require java
  class {'agent':}
  class {'php':}
  
  #install agent before php
  Class['agent'] ~> Class['php']
}

# loadbalancer cartridge node
node /lb/ inherits base {
  require java
  class {'agent':}
  class {'lb': maintenance_mode   => 'norestart',}
}

# tomcat cartridge node
node /tomcat/ inherits base {
  require java
  class {'agent':}
  class {'tomcat':}
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
  Class['agent'] ~> Class['nodejs']
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
#  Class['agent'] ~> Class['ruby']
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

