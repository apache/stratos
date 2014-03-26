# --------------------------------------------------------------
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# --------------------------------------------------------------
#
# Class: cc
#
# This class installs Stratos cloud controller
#
#
# Actions:
#   - Install Stratos cloud controller
#
# Requires:
#
# Sample Usage:
#

class cc (
  $version            = '4.0.0',
  $offset             = 0,
  $tribes_port        = 4000,
  $maintenance_mode   = true,
  $members            = {},
  $owner              = 'root',
  $group              = 'root',
  $target             = '/mnt',
) inherits params {


  $deployment_code = 'cc'
  $carbon_version  = $version
  $service_code    = 'cc'
  $carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"

  $service_templates = [
    'conf/axis2/axis2.xml',
    'conf/carbon.xml',
    'conf/jndi.properties',
    'conf/cloud-controller.xml',
    ]

  tag($service_code)

  cc::clean { $deployment_code:
    mode   => $maintenance_mode,
    target => $carbon_home,
  }

  cc::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    mode      => $maintenance_mode,
    owner     => $owner,
    require   => Cc::Clean[$deployment_code],
  }

  cc::deploy { $deployment_code:
    service  => $deployment_code,
    security => true,
    owner    => $owner,
    group    => $group,
    target   => $carbon_home,
    require  => Cc::Initialize[$deployment_code],
  }

  cc::push_templates {
    $service_templates:
      target    => $carbon_home,
      directory => $deployment_code,
      require   => Cc::Deploy[$deployment_code];
  }

  cc::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
      Cc::Initialize[$deployment_code],
      Cc::Deploy[$deployment_code],
      Cc::Push_templates[$service_templates],
      ],
  }
}
