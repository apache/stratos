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

# Jboss register service

define jboss::service  (
  $product      = undef,
  $user         = undef,
  $group        = undef,
  $version      = undef,
  $java_home    = '/usr/java/latest',
  $java_opts    = '',
  $home         = '/home',
  $bind_address = $::fqdn,
  $product_dir  = undef,
) {

  $jboss_properties = "${home}/${user}/${product}/jboss.properties"  
  $stratos_ssl_cert = "${home}/${user}/${product}/cert.pem"
  $stratos_ssl_key = "${home}/${user}/${product}/key.pem"
  $stratos_ssl_keystore = "${home}/${user}/${product}/jboss.jks"
  $keystore_passwd = "changeit"
  $keystore_alias = "jboss"

  # defaults
  File {
    owner => $user,
    group => $group,
  }

  file { "${home}/${user}/${product}":
    ensure => directory,   
    mode   => '0750',
  }

  file { "${home}/${user}/${product}/run":
    ensure  => present,
    mode    => '0555',    
    content => template("jboss/${product}/run.erb"),
    require => File["${home}/${user}/${product}"],
  }

  file { $jboss_properties:
    ensure  => present,
    mode    => '755',
    owner   => $user,
    group   => $group,
    content => template("jboss/${product}/jboss.properties.erb"),
    require => File["${home}/${user}/${product}"],
  }

  file { "${product_dir}/standalone/configuration/standalone-full.xml":
    ensure  => present,
    mode    => '755',
    owner   => $user,
    group   => $group,
    content => template("jboss/${product}/standalone-full.xml.erb"),
    require => File["${product_dir}"],
  } 

  jboss::importssl { "${user}-${product}-ssl" :
     user                 => $user,
     stratos_ssl_cert     => $stratos_ssl_cert,
     stratos_ssl_key      => $stratos_ssl_key,
     import_type          => 'jks',
     stratos_ssl_keystore => $stratos_ssl_keystore,
     keystore_alias       => $keystore_alias,
     keystore_passwd      => $keystore_passwd, 
     product              => $product,
     java_home            => $java_home,
     require              => File["${home}/${user}/${product}"],
  }

  exec { "${user}-${product}-${version}":
    path        => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
    cwd         => "${home}/${user}/${product}",
    command     => "nohup bash run &",
    user        => $user,
    require     => [
                     File["${home}/${user}/${product}/run"],
                     File["${home}/${user}/${product}/jboss.properties"],
                     File["${product_dir}/standalone/configuration/standalone-full.xml"],
                     Jboss::Importssl["${user}-${product}-ssl"]
                   ]
  }
 
}
