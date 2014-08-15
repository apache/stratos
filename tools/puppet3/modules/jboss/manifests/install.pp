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

# Jboss install product

define jboss::install (
  $version,
  $user,
  $group,
  $basedir,
  $home = '/home',
  $product = undef
) {
  $packfile = "${version}.tar.gz"
  $subdir  = $version
  if ! defined(Package['unzip']) {
    package { 'unzip': ensure => installed }
  }
  if ! defined(Package['rsync']) {
    package { 'rsync': ensure => installed }
  }
  if ! defined(Package['sed']) {
    package { 'sed': ensure => installed }
  }
  if ! defined(Package['tar']) {
    package { 'tar': ensure => installed }
  }

  # defaults
  File {
    owner => $user,
    group => $group,
  }

  file { "${basedir}" :
    ensure => directory,   
    mode   => '0750',
  }

  file { "jboss-pack-${version}":
    ensure  => present,
    path    => "${basedir}/${packfile}",
    mode    => '0444',
    source  => "puppet:///modules/jboss/${packfile}",
    require => File["${basedir}"],
  }

  exec { "jboss-unpack-${version}":
    path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
    cwd     => $basedir,
    command => "tar -xvzf '${basedir}/${packfile}'",
    creates => "${basedir}/${subdir}",
    notify  => Exec["jboss-fix-ownership-${version}"],
    require => File["jboss-pack-${version}"],
  }

  file { "${basedir}/${subdir}":
    ensure  => directory,
    require => Exec["jboss-unpack-${version}"],
  }   

  exec { "jboss-fix-ownership-${version}":
    path        => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
    command     => "chown -R ${user}:${group} ${basedir}/${subdir}",
    refreshonly => true,
  }

}
