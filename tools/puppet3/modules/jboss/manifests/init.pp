#--------------------------------------------------------------
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
#--------------------------------------------------------------

# Jboss main class

class jboss (
   $user         = 'jbossas1',
   $group        = 'jboss',
   $java_home    = undef,
   $java_opts    = '',
   $version      = undef,
   $basedir      = '/mnt',
   $bind_address = '0.0.0.0',
   $java_opts    = '',
   $extra_jars   = []
) {

  require java

  $custom_agent_templates = ['extensions/artifacts-updated.sh']
  class {'python_agent':
    custom_templates => $custom_agent_templates,
    module=>'jboss'
  }

  include '::jboss::config'  

  # Application Server
  jboss::as { "jbossas1":
    extra_jars     => $extra_jars,    
    group          => $group,
    java_home      => $java_home,
    version        => $version,
    basedir        => $basedir,
    bind_address   => $bind_address,
    java_opts      => $java_opts,
  }
  
  #install stratos_base before java before jboss before agent
  Class['stratos_base'] -> Class['java'] -> Class['python_agent'] -> Class['jboss']
}
