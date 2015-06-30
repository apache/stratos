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

# Initializing the deployment

define python_agent::initialize ($repo, $version, $agent_name, $local_dir, $target, $owner,) {

  $packages = ['python-dev', 'python-pip', 'gcc']

  package { $packages:
    ensure => installed,
    provider => 'apt',
  }

  exec {
    "pip installs-paho":
        path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        command => "pip install paho-mqtt",
        require => Package[$packages];

    "pip installs-GitPython==0.3.1-beta2":
        path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        command => "pip install GitPython==0.3.1-beta2",
        require => Exec["pip installs-paho"];

    "pip installs-psutil":
        path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        command => "pip install psutil",
        require => Exec["pip installs-GitPython==0.3.1-beta2"];

    "pip installs-gittle":
        path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        command => "pip install gittle",
        require => Exec["pip installs-psutil"];

    "pip installs-pexpect":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "pip install pexpect",
      require => Exec["pip installs-gittle"];

    "pip installs-yapsy":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "pip install yapsy",
      require => Exec["pip installs-pexpect"];
  }

  exec {
    "creating_target_for_python_${name}":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${target}",
      require => Exec["pip installs-pexpect"];

    "creating_local_package_repo_for_python_${name}":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/',
      unless  => "test -d ${local_dir}",
      command => "mkdir -p ${local_dir}",
        require => Exec["creating_target_for_python_${name}"];
  }

  file {
    "/${local_dir}/${agent_name}.zip":
      ensure => present,
      source => ["puppet:///modules/python_agent/${agent_name}.zip"],
      require   => Exec["creating_local_package_repo_for_python_${name}"],
  }

  exec {
    "extracting_${agent_name}.zip_for_${name}":
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => "$target",
      unless    => "test -d ${target}/${agent_name}/agent.conf",
      command   => "unzip -o ${local_dir}/${agent_name}.zip",
      logoutput => 'on_failure',
      require   => File["/${local_dir}/${agent_name}.zip"];

    "setting_permission_for_python_${name}":
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      command   => "chown -R ${owner}:${owner} ${target} ;
                    chmod -R 755 ${target}",
      logoutput => 'on_failure',
      require   => Exec["extracting_${agent_name}.zip_for_${name}"];
  }
}
