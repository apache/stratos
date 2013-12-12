#!/bin/bash
# --------------------------------------------------------------
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
# --------------------------------------------------------------

export JAVA_HOME=/opt/java
export PATH=$PATH:$JAVA_HOME/bin

keystore=/mnt/<%= server_ip %>/<%= service_dir %>/repository/resources/security/stratos-jar-sign.jks
keyalias=stratoslive
keypass='TdTXVW7mzxu3oqWaHYm2mEgxoX88hHCVKMJ7K1rp'
root=/mnt/<%= server_ip %>/<%= service_dir %>

for p in $root/repository/components/plugins $root/repository/components/lib $root/lib $root/lib/patches $root/lib/api $root/lib/xboot $root/bin $root/lib/xboot
do
	for i in `find -iname "*.jar"`
	do
		echo $i
		jarsigner -keystore $keystore -storepass $keypass $i $keyalias
	done
done

