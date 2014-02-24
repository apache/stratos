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
# Class: messagebroker
#
# This class installs wso2 Message Broker
#
#
# Actions:
#   - Install wso2 Message Broker
#
# Requires:
#
# Sample Usage:
#

class messagebroker (
  $sub_cluster_domain = undef,
  $version            = '2.1.0',
  $offset             = 0,
  $tribes_port        = 4000,
  $config_db          = 'governance',
  $maintenance_mode   = true,
  $depsync            = false,
  $clustering         = false,
  $members            = {},
  $owner              = 'root',
  $group              = 'root',
  $target             = '/mnt',
) inherits params {


  $deployment_code = 'messagebroker'
  $carbon_version  = $version
  $service_code    = 'mb'
  $carbon_home     = "${target}/wso2${service_code}-${carbon_version}"

  $service_templates = [
    'conf/axis2/axis2.xml',
    'conf/carbon.xml',
  ]

  tag($service_code)

  messagebroker::clean { $deployment_code:
    mode   => $maintenance_mode,
    target => $carbon_home,
  }

  messagebroker::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    mode      => $maintenance_mode,
    owner     => $owner,
    require   => Messagebroker::Clean[$deployment_code],
  }

  messagebroker::deploy { $deployment_code:
    service  => $deployment_code,
    security => true,
    owner    => $owner,
    group    => $group,
    target   => $carbon_home,
    require  => Messagebroker::Initialize[$deployment_code],
  }

  messagebroker::push_templates {
    $service_templates:
      target    => $carbon_home,
      directory => $deployment_code,
      require   => Messagebroker::Deploy[$deployment_code];
  }

  messagebroker::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
      Messagebroker::Initialize[$deployment_code],
      Messagebroker::Deploy[$deployment_code],
      Messagebroker::Push_templates[$service_templates],
      ],
  }
}
