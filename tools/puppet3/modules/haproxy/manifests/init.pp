#--------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#
#--------------------------------------------------------------

class haproxy(
  $network_partition_id = $stratos_network_partition_id,
  $service_filter       = $stratos_load_balanced_service_type,
  $cluster_id           = $stratos_cluster_id,
  $service_name         = $stratos_instance_data_service_name,
  $lb_service_type      = $stratos_instance_data_lb_service_type,
  $version              = '4.1.0',
  $owner                = 'root',
  $group                = 'root',
  $target               = '/mnt',
){

  $deployment_code = 'haproxy-extension'
  $carbon_version  = $version
  $service_code    = 'haproxy-extension'
  $carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"

  tag($service_code)

  require java
  class {'python_agent':}

  $service_templates = [
    'bin/haproxy-extension.sh',
    'conf/jndi.properties',
    ]

  package { ['haproxy', 'socat']:
    ensure  => installed,
  }

  haproxy::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    owner     => $owner,
  }
  
  haproxy::push_templates {
    $service_templates:
      target    => $carbon_home,
      require   => Haproxy::Initialize[$deployment_code];
  }

  haproxy::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
      Package['haproxy'],
      Haproxy::Push_templates[$service_templates],
    ];
  }

  # install stratos_base before java before haproxy before agent
  Class['stratos_base'] -> Class['java'] -> Class['python_agent'] -> Class['haproxy']
}
