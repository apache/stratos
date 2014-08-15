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

# Imports the given self-signed SSL certificate to PHP instance

define php::importssl ($ssl_certificate_file, $ssl_key_file) {

   if $ssl_enabled == 'true' and $ssl_certificate_file and $ssl_key_file{
        $crt_file = file( $ssl_certificate_file , '/dev/null' )
        if($crt_file != '') {
           file { '/etc/ssl/certs/stratos-ssl-cert.pem':
              content => $crt_file,
              mode => 600
           }
        }
       
        $key_file = file( $ssl_key_file , '/dev/null' )
        if($key_file != '') {
           file { '/etc/ssl/private/stratos-ssl-key.pem':
              content => $key_file,
              mode => 600
           }
        }

    }
}
