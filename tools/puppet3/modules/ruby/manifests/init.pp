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

class ruby( $target = '/mnt' ) {

  $custom_agent_templates = ['extensions/instance-started.sh']
  class {'python_agent':
    custom_templates => $custom_agent_templates,
    module=>'ruby'
  }

  if $stratos_app_path {
    $ruby_home = $stratos_app_path
  } 
  else {
    $ruby_home = "${target}/ruby"
  }

  file { '/mnt/ruby-start.sh':
    ensure  => present,
    mode    => '0755',
    content => template('ruby/ruby-start.sh.erb'),
    require => Exec['Create ruby home'];
  }

  exec {
    'Create ruby home':
      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      command => "mkdir -p ${ruby_home}";

#    'Create ruby application':
#      path    => '/usr/local/rvm/gems/ruby-2.1.0/bin:/usr/local/rvm/gems/ruby-2.1.0@global/bin:/usr/local/rvm/rubies/ruby-2.1.0/bin',
#      cwd     => $ruby_home,
#      command => "rails new app",
#      require => Exec['Create ruby home'];
    
#    'Clone repo':
#      path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
#      command => "git clone ${stratos_git_repo} ${ruby_home}/app",
#      require => Exec['Create ruby home'];

#    'Run script':
#      path    => '/usr/local/rvm/gems/ruby-2.1.0/bin:/usr/local/rvm/gems/ruby-2.1.0@global/bin:/usr/local/rvm/rubies/ruby-2.1.0/bin',
#      cwd     => "${ruby_home}/app",
#      command => "rails server > /dev/null 2>&1 &",
#      require => Exec['Create ruby application'];

    'Run start script':
      command => "/bin/bash /mnt/ruby-start.sh",
      require => File['/mnt/ruby-start.sh'];
  }

  # install stratos_base before java before ruby before agent
  Class['stratos_base'] -> Class['python_agent'] -> Class['ruby']
}
