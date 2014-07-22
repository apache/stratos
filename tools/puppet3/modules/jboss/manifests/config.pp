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

# Jboss configuration class

class jboss::config {
  # TODO: Find recommended values
  augeas { 'jboss-sysctl':
    context => '/files/etc/sysctl.conf',
    changes => [
      'set net.ipv4.tcp_fin_timeout 30',
      'set fs.file-max 2097152',
      'set net.ipv4.tcp_tw_recycle 1',
      'set net.ipv4.tcp_tw_reuse 1',
      'set net.core.rmem_default 524288',
      'set net.core.wmem_default 524288',
      'set net.core.rmem_max 67108864',
      'set net.core.wmem_max 67108864',
      'set net.ipv4.tcp_rmem 4096 87380 16777216',
      'set net.ipv4.tcp_wmem 4096 65536 16777216',
    ],
  }
  #limits::set { '@jboss':
  #  item   => 'nofile',
  #  soft   => '4096',
  #  hard   => '65535'
  #}
}
