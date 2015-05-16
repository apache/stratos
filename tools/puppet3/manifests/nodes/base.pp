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

# Stratos base node
node 'base' {

  #essential variables
  $package_repo         = 'http://10.4.128.7'
  $local_package_dir    = '/mnt/packs'
  $mb_url		= 'tcp://127.0.0.1:1883'
  $mb_type		= 'activemq' #in wso2 mb case, value should be 'wso2mb'
  $cep_ip               = '127.0.0.1'
  $cep_port             = '7711'
  $cep_username       ='admin'
  $cep_password       ='admin'
  $truststore_password  = 'wso2carbon'
  $java_distribution	= 'jdk-7u51-linux-x64.tar.gz'
  $java_name		= 'jdk1.7.0_51'
  $member_type_ip       = 'private'
  $lb_httpPort          = '80'
  $lb_httpsPort         = '443'
  $tomcat_version       = '7.0.52'
  $enable_log_publisher = 'false'
  $bam_ip		= '127.0.0.1'
  $bam_port		= '7611'
  $bam_secure_port	= '7711'
  $bam_username		= 'admin'
  $bam_password		= 'admin'
  #metadata_service_url should be 'https://SM-IP:SM-Port'
  $metadata_service_url = 'https://127.0.0.1:9443'

  require stratos_base 
}
