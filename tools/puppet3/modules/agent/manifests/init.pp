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
  $version                = '4.1.0',
  $owner                  = 'root',
  $group                  = 'root',
  $target                 = "/mnt",
  $type                   = 'default',
  $enable_artifact_update = true,
  $auto_commit            = false,
  $auto_checkout          = true,
  $module                 = 'undef',
  $custom_templates       = [],
  $exclude_templates	  = []
){

  $deployment_code = 'cartridge-agent'
  $carbon_version  = $version
  $service_code    = 'cartridge-agent'
  $carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"

  tag($service_code)

  $default_templates = [
    'bin/stratos.sh',
    'conf/jndi.properties',
    'conf/log4j.properties',   
    'conf/mqtttopic.properties',   
    'extensions/clean.sh',
    'extensions/instance-activated.sh',
    'extensions/instance-started.sh',
    'extensions/start-servers.sh',
    'extensions/artifacts-copy.sh',
    'extensions/artifacts-updated.sh',
    'extensions/complete-tenant.sh',
    'extensions/complete-topology.sh',
    'extensions/member-activated.sh',
    'extensions/member-suspended.sh',
    'extensions/member-terminated.sh',
    'extensions/mount-volumes.sh',
    'extensions/domain-mapping-added.sh',
    'extensions/domain-mapping-removed.sh',
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

  # excluding templates which are not needed by a cartridge module from default_templates
  $default_templates_excluded = difference($default_templates,$exclude_templates)

  # excluding custom_templates, if any,(which will be overrided by a cartridge module) from default_templates
  $service_templates = $module ? {
     'undef'    => $default_templates_excluded,
      default   => difference($default_templates_excluded,$custom_templates)
  }

  # applying default extensions
  agent::push_templates {
    $service_templates:
      target    => $carbon_home,
      template_dir => "agent",
      require   => Agent::Initialize[$deployment_code];
  }

  # applying custom extensions
  unless $module == 'undef' {
    agent::push_templates {
      $custom_templates:
        target    => $carbon_home,
        template_dir => "${module}/agent",
        require   => [Agent::Initialize[$deployment_code]]
    }
  }

  # removing default extensions which are shipped by agent.zip
  agent::remove_templates {
    $exclude_templates:
      target    => $carbon_home,
  }

  $required_resources = $module ? {
    'undef'  => [
            Exec['copy launch-params to carbon_home'],
            Agent::Push_templates[$default_templates_excluded],
           ],
     default =>[
            Exec['copy launch-params to carbon_home'],
            Agent::Push_templates[$default_templates_excluded],
            Agent::Push_templates[$custom_templates]         ]
  }

  agent::start { $deployment_code:
    owner   => $owner,
    target  => $carbon_home,
    require => $required_resources
  }
}
