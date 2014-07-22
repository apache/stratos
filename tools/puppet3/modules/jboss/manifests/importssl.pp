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

# Imports the given self-signed SSL certificate to Jboss keystore

define jboss::importssl (  
  $stratos_ssl_key      = undef,
  $stratos_ssl_cert     = undef,
  $stratos_ssl_keystore = undef,    
  $keystore_passwd      = 'changeit',
  $keystore_alias       = 'jboss',
  $home                 = '/home',
  $user                 = undef,
  $import_type          = 'native',
  $product              = undef,
  $java_home            = '/usr/java/latest'
){

   if $ssl_certificate_file and $ssl_key_file{
        $crt_file = file( $ssl_certificate_file , '/dev/null' )
        if($crt_file != '') {
           file { "${stratos_ssl_cert}" :
              owner     => $user,
              content  => $crt_file,
              mode     => 644
           }
        }
       
        $key_file = file( $ssl_key_file , '/dev/null' )
        if($key_file != '') {
           file { "${stratos_ssl_key}" :
              owner     => $user,
              content  => $key_file,
              mode     => 600
           }
        }

     if $import_type == 'jks' {

        exec { 'create PKCS12 keystore':
           user      => $user,
           cwd       => "${home}/${user}/${product}",
           path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/',              
           command   => "openssl pkcs12 -export -name ${keystore_alias} -in ${stratos_ssl_cert} -inkey ${stratos_ssl_key} -password pass:${keystore_passwd} -out ${home}/${user}/${product}/keystore.p12",
           require   => [
                           File["${stratos_ssl_cert}"],
                           File["${stratos_ssl_key}"],
                        ]; 
        }

        exec { 'import ssl to jks':
           user      => $user,
           cwd       => "${home}/${user}/${product}",
           path      => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/bin/',              
           command   => "${java_home}/bin/keytool -noprompt -importkeystore -destkeystore ${stratos_ssl_keystore} -srckeystore ${home}/${user}/${product}/keystore.p12 -srcstoretype pkcs12 -alias ${keystore_alias} -deststorepass ${keystore_passwd} -srcstorepass ${keystore_passwd}",
           require   => Exec["create PKCS12 keystore"]                        
        }
      }

    }
}

