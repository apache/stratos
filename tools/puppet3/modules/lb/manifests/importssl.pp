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

# Imports the given self-signed SSL certificate to LB client trust store

define lb::importssl ($owner, $target, $ssl_certificate_file) {
  
   $path_suffix = '/repository/resources/security'
   $target_cwd = "${target}${path_suffix}"
   $dest_filename = 'stratos-ssl-cert.pem'
   $dest_file_path = "${target_cwd}/${dest_filename}"

   if $ssl_enabled == 'true' and $ssl_certificate_file {
        $crt_file = file( $ssl_certificate_file , '/dev/null' )
        if($crt_file != '') {
          file {  $dest_file_path  :
             content => $crt_file,
             ensure  => present,
             mode    => 755
          }

          exec { "import ssl certificate":
              user    => $owner,
              path    => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/',
              cwd     => $target_cwd,
              command => 'keytool -noprompt -import -file stratos-ssl-cert.pem -keystore client-truststore.jks -storepass wso2carbon -alias stratos-cert-ks',
              require => File[$dest_file_path]
          }
       }
   }
}
