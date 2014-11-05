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

# Class: lb
#
# This class installs Stratos Load balancer
#
#
# Actions:
#   - Install Stratos Load balancer
#
# Requires:
#
# Sample Usage:
#

class lb (
  $version            = '4.0.0',
  $offset             = 0,
  $tribes_port        = 4000,
  $maintenance_mode   = true,
  $members            = {},
  $owner              = 'root',
  $group              = 'root',
  $target             = '/mnt',
) inherits params {

  $deployment_code = 'lb'
  $carbon_version  = $version
  $service_code    = 'load-balancer'
  $carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"

  require java
  class {'python_agent':}

  $service_templates = [
    'conf/axis2/axis2.xml',
    'conf/loadbalancer.conf',
    'conf/templates/jndi.properties.template',    
    ]

  tag($service_code)

  lb::clean { $deployment_code:
    mode   => $maintenance_mode,
    target => $carbon_home,
  }

  lb::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    mode      => $maintenance_mode,
    owner     => $owner,
    require   => Lb::Clean[$deployment_code],
  }

  lb::deploy { $deployment_code:
    service  => $deployment_code,
    security => true,
    owner    => $owner,
    group    => $group,
    target   => $carbon_home,
    require  => Lb::Initialize[$deployment_code],
  }

  lb::push_templates {
    $service_templates:
      target    => $carbon_home,
      directory => $deployment_code,
      require   => Lb::Deploy[$deployment_code];
  }

  lb::importssl  { $deployment_code:
    owner                 => $owner,
    target                => $carbon_home,
    ssl_certificate_file => $ssl_certificate_file,
    require               => [
       Lb::Initialize[$deployment_code],
       Lb::Deploy[$deployment_code],
       Lb::Push_templates[$service_templates]
     ]
  }

  lb::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
       Lb::Initialize[$deployment_code],
       Lb::Deploy[$deployment_code],
       Lb::Push_templates[$service_templates],
       Lb::Importssl[$deployment_code]
     ],
   }

  # install stratos_base before java before lb before agent
  Class['stratos_base'] -> Class['java'] -> Class['python_agent'] -> Class['lb']
}
