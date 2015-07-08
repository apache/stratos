# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------
#
# Class: appserver
#
# This class installs WSO2 Appserver
#
# Parameters:
# version            => '5.2.1'
# offset             => 1,
# hazelcast_port        => 4100,
# config_db          => 'as_config',
# maintenance_mode   => 'zero',
# depsync            => false,
# sub_cluster_domain => 'mgt',
# clustering         => true,
# owner              => 'root',
# group              => 'root',
# target             => '/mnt/${server_ip}/',
# members            => {'elb2.wso2.com' => 4010, 'elb.wso2.com' => 4010 }
#
# Actions:
#   - Install WSO2 Appserver
#
# Requires:
#
# Sample Usage:
#

class appserver (
  $version            = undef,
  $sub_cluster_domain = undef,
  $members            = undef,
  $offset             = 0,
  $hazelcast_port     = 4000,
  $config_db          = 'governance',
  $config_target_path = 'config/as',	
  $maintenance_mode   = true,
  $depsync            = false,
  $clustering         = false,
  $cloud              = true,
  $owner              = 'root',
  $group              = 'root',
  $target             = "/mnt/${server_ip}",
) inherits params {

  $deployment_code = 'appserver'
  $carbon_version  = $version
  $service_code    = 'as'
  $carbon_home     = "${target}/wso2${service_code}-${carbon_version}"

  $service_templates = $sub_cluster_domain ? {
    'mgt'    => [
      'conf/axis2/axis2.xml',
      'conf/carbon.xml',
#      'conf/datasources/master-datasources.xml',
#      'conf/registry.xml',
#      'conf/tomcat/catalina-server.xml',
#      'conf/user-mgt.xml',
      ],
    'worker' => [
      'conf/axis2/axis2.xml',
      'conf/carbon.xml',
#      'conf/datasources/master-datasources.xml',
#      'conf/registry.xml',
#      'conf/tomcat/catalina-server.xml',
#      'conf/user-mgt.xml',
      ],
    default => [
      'conf/axis2/axis2.xml',
      'conf/carbon.xml',
      'conf/datasources/master-datasources.xml',
      'conf/registry.xml',
      'conf/tomcat/catalina-server.xml',
      'conf/user-mgt.xml',
      'conf/log4j.properties',
      'conf/security/authenticators.xml',
      ],
  }

  tag($service_code)

  appserver::clean { $deployment_code:
    mode   => $maintenance_mode,
    target => $carbon_home,
  }

  appserver::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    mode      => $maintenance_mode,
    owner     => $owner,
    require   => Appserver::Clean[$deployment_code],
  }

  appserver::deploy { $deployment_code:
    security => true,
    owner    => $owner,
    group    => $group,
    target   => $carbon_home,
    require  => Appserver::Initialize[$deployment_code],
  }

  appserver::push_templates {
    $service_templates:
      target    => $carbon_home,
      directory => $deployment_code,
      require   => Appserver::Deploy[$deployment_code];
  }

#  appserver::start { $deployment_code:
#    owner   => $owner,
#    target  => $carbon_home,
#    require => [
#      Appserver::Initialize[$deployment_code],
#      Appserver::Deploy[$deployment_code],
#      Push_templates[$service_templates],
#      ],
#  }

}
