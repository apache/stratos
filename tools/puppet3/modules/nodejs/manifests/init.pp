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

class nodejs {

  $custom_agent_templates = ['extensions/start-servers.sh']
  class {'python_agent':
    custom_templates => $custom_agent_templates,
    module=>'nodejs'
  }

  $target = '/mnt'

  if $stratos_app_path {
    $nodejs_home = $stratos_app_path
  } 
  else {
    $nodejs_home = "${target}/nodejs"
  }

  package { ['python-software-properties', 'python', 'g++', 'make']:
    ensure => installed,
  }


  exec {
    'update-apt':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'apt-get update > /dev/null 2>&1';

    'add-repo':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'add-apt-repository ppa:chris-lea/node.js > /dev/null 2>&1';

    'Create nodejs home':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${nodejs_home}";
    
    'Install libraries':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd     => "${nodejs_home}",
      command => 'apt-get install -y nodejs',
      require => [
        Exec['update-apt'], 
        Package['python-software-properties', 'python', 'g++', 'make'],
	Exec['add-repo'],
	Exec['update-apt'],
	Exec['Create nodejs home'],
      ];

    'Start application':
      path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      cwd       => "${nodejs_home}",
      onlyif    => 'test -f web.js',
      command   => 'node web.js > /dev/null 2>&1 &',
      tries     => 100,
      try_sleep => 2,
      require   => Exec['Install libraries'];
  }

  # install stratos_base before java before nodejs before agent
  Class['stratos_base'] -> Class['python_agent'] -> Class['nodejs']
}

