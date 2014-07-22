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

# Jboss HornetQ type

define jboss::hornetq (
  $bind_address = $::fqdn,
  $extra_jars   = [],
  $group        = 'jboss',
  $home         = '/home',
  $java_home    = '/usr/java/latest',
  $java_opts    = '',
  $version      = undef,
) {
  $user        = $title
  $product     = 'hornetq'
  $product_dir = "${home}/${user}/${product}-${version}"

  if ! defined(File["/etc/runit/${user}"]) {
    runit::user { $user: group => $group }
  }

  jboss::install { "${user}-${product}":
    version     => "${product}-${version}",
    user        => $user,
    group       => $group,
    basedir     => "${home}/${user}",
  }

  $file_paths = prefix($extra_jars, "${product_dir}/")
  jboss::extra_jars { $file_paths:
    product_dir => $product_dir,
    destination => "${product_dir}/standalone/lib/ext",
    user        => $user,
    require     => File[$product_dir],
  }

  jboss::service{ "${user}-${product}":
    product      => $product,
    user         => $user,
    group        => $group,
    version      => $version,
    java_home    => $java_home,
    java_opts    => $java_opts,
    home         => $home,
    bind_address => $bind_address,
  }
}
