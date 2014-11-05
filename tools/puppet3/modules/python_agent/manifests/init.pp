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

class python_agent(
  $version                = '1.0.0',
  $owner                  = 'root',
  $group                  = 'root',
  $target                 = "/mnt",
  $type                   = 'default',
  $enable_artifact_update = true,
  $auto_commit            = false,
  $auto_checkout          = true,
){

  $deployment_code = 'cartridge-agent'
  #$carbon_version  = $version
  $service_code    = 'cartridge-agent'
  #$carbon_home     = "${target}/apache-stratos-${service_code}-${carbon_version}"
  $agent_home= "${target}/${service_code}"

  tag($service_code)

  $service_templates = [
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
    'extensions/subscription-domain-added.sh',
    'extensions/subscription-domain-removed.sh',
    ]

  python_agent::initialize { $deployment_code:
    repo      => $package_repo,
    version   => $version,
    service   => $service_code,
    local_dir => $local_package_dir,
    target    => $target,
    owner     => $owner,
  }

  exec { 'copy launch-params to python agent-payload':
    path    => '/bin/',
    command => "mkdir -p ${target}/${service_code}/payload; cp /tmp/payload/launch-params ${target}/${service_code}/payload/launch-params",
    require => Py_Agent::Initialize[$deployment_code],
  }

  exec { 'make extension folder':
    path    => '/bin/',
    command => "mkdir -p ${target}/${service_code}/extensions",
    require => Exec["copy launch-params to python agent-payload"],
  }

  python_agent::push_templates {  $service_templates:
      target    => $agent_home,
      require   => Exec["make extension folder"],
  }

  file { "${target}/${service_code}/agent.conf":
    ensure => file,
    content => template("python_agent/agent.conf.erb"),
    require => Py_Agent::Push_Templates[$service_templates],
  }

  file { "${target}/${service_code}/logging.ini":
    ensure => file,
    content => template("python_agent/logging.ini.erb"),
    require => File["${target}/${service_code}/agent.conf"],
  }

  python_agent::start { $deployment_code:
    owner   => $owner,
    target  => "${target}/${service_code}",
    require => File["${target}/${service_code}/logging.ini"],
  }
}
