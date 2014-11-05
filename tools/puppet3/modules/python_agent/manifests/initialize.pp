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

define python_agent::initialize ($repo, $version, $service, $local_dir, $target, $owner,) {

  exec { "updates":
        path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        command => "apt-get update",
  }

  $packages = ['python-dev', 'python-pip', 'gcc']

  package { $packages:
    ensure => installed,
    provider => 'apt',
    require => Exec["updates"],
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
  }

  exec { 'cleanup-python-agent':
    path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
    command => "rm -rf /${local_dir};rm -rf ${target}/${service};",
    require => Exec["pip installs-gittle"];
  }

  exec {
    "creating_target_for_python_${name}":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${target}",
      require => Exec["cleanup-python-agent"];

    "creating_local_package_repo_for_python_${name}":
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/',
      unless  => "test -d ${local_dir}",
      command => "mkdir -p ${local_dir}",
        require => Exec["creating_target_for_python_${name}"];
  }

  file {
    "/${local_dir}/${service}.zip":
      ensure => present,
      source => ["puppet:///modules/python_agent/${service}.zip"],
      require   => Exec["creating_local_package_repo_for_python_${name}"],
  }

  exec {
    "extracting_${service}.zip_for_${name}":
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      #/mnt/cartridge-agent/agent.py
      unless    => "test -d ${target}/${service}/agent.conf",
      command   => "unzip -o ${local_dir}/${service}.zip",
      logoutput => 'on_failure',
      require   => File["/${local_dir}/${service}.zip"];

    "setting_permission_for_python_${name}":
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => $target,
      command   => "chown -R ${owner}:${owner} ${target}/${service} ;
                    chmod -R 755 ${target}/${service}",
      logoutput => 'on_failure',
      require   => Exec["extracting_${service}.zip_for_${name}"];
  }
}
