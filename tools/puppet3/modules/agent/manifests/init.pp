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

class agent(
  $version = '4.0.0-incubating',
  $owner   = 'root',
  $group   = 'root',
  $target  = '/mnt',
  $type    = 'default',
){

  $deployment_code = 'cartridge-agent'
  $carbon_version  = $version
  $service_code    = 'cartridge-agent'
  $carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"

  tag($service_code)

  $service_templates = [
    'bin/stratos.sh',
    'conf/templates/jndi.properties.template',
    'extensions/artifacts-updated.sh',
    'extensions/clean.sh',
    'extensions/instance-activated.sh',
    'extensions/instance-started.sh',
    'extensions/start-servers.sh',
    ]

  agent::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $carbon_version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    owner     => $owner,
  }

  exec { 'copy launch-params to carbon_home':
    path    => '/bin/',
    command => "mkdir -p ${carbon_home}/payload; cp /tmp/payload/launch-params ${carbon_home}/payload/launch-params",
    require => Agent::Initialize[$deployment_code];
  }

  agent::push_templates {
    $service_templates:
      target    => $carbon_home,
      require   => Agent::Initialize[$deployment_code];
  }

  agent::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => [
      Exec['copy launch-params to carbon_home'],
      Agent::Push_templates[$service_templates],
    ];
  }
}
