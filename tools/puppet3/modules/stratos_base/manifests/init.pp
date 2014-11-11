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

class stratos_base(
  $ensure = 'present',
  $autoupgrade=true, 
){ 

  if ! ($ensure in [ "present", "absent" ]) {
    fail("ensure parameter must be absent or present")
  }

  if ! ("$autoupgrade" in [ 'true', 'false' ]) {
    fail("autoupgrade parameter must be true or false")
  }

# Set local variables based on the desired state
  if $ensure == "present" {
    if $autoupgrade == true {
      $package_ensure = latest      
    } 
    else {
      $package_ensure = present     
    }
  } 
  else {        
      $package_ensure = absent      
  }

  exec { 'update-apt':
    path      => ['/bin/', '/sbin/', '/usr/bin/', '/usr/sbin/', '/usr/local/bin/', '/usr/local/sbin/'],
    command   => 'apt-get update > /dev/null 2>&1',
    logoutput => on_failure,
    require   => File['/etc/apt/apt.conf.d/90forceyes'];
  }

  $packages = [
    'nano',       
    'curl',
    'wget',    
    'zip',
    'unzip',
    'tar']

  package { $packages:
    ensure => $package_ensure,
    require => Exec['update-apt'],
  }

  define printPackages{
    notify { $name: 
      message => "Installed package: ${name}",
    }
  }
  printPackages{ $packages:}

}
