# ----------------------------------------------------------------------------
#  Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# ----------------------------------------------------------------------------
#
# Class: cep
#
# This class installs wso2 complex event processor
#
#
# Actions:
#   - Install wso2 complex event processor
#
# Requires:
#
# Sample Usage:
#

class cep (
  $sub_cluster_domain = undef,
  $version            = '3.0.0',
  $offset             = 0,
  $hazelcast_port     = 4000,
  $config_db          = 'governance',
  $maintenance_mode   = true,
  $depsync            = false,
  $clustering         = false,
  $members            = {},
  $owner              = 'root',
  $group              = 'root',
  $target             = '/mnt',
) inherits params {


  $deployment_code = 'cep'
  $carbon_version  = $version
  $service_code    = 'cep'
  $carbon_home     = "${target}/wso2${service_code}-${carbon_version}"

  $service_templates = [
    'conf/axis2/axis2.xml',
    'conf/carbon.xml',
    'conf/jndi.properties',
    'deployment/server/outputeventadaptors/JMSOutputAdaptor.xml',
  ]

  tag($service_code)

  cep::clean { $deployment_code:
    mode   => $maintenance_mode,
    target => $carbon_home,
  }

  cep::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    mode      => $maintenance_mode,
    owner     => $owner,
    require   => Cep::Clean[$deployment_code],
  }

  cep::deploy { $deployment_code:
    service  => $deployment_code,
    security => true,
    owner    => $owner,
    group    => $group,
    target   => $carbon_home,
    require  => Cep::Initialize[$deployment_code],
  }

  cep::push_templates {
    $service_templates:
      target    => $carbon_home,
      directory => $deployment_code,
      require   => Cep::Deploy[$deployment_code];
  }

  cep::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
      Cep::Initialize[$deployment_code],
      Cep::Deploy[$deployment_code],
      Cep::Push_templates[$service_templates],
      ],
  }
}
