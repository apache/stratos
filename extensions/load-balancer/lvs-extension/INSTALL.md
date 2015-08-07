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
# This is a generated file and will be overwritten at the next load balancer startup.
# Please use loadbalancer.conf for updating mb-ip, mb-port and templates/jndi.properties.template
# file for updating other configurations.
#
# Installing Apache Stratos LVS Extension

Apache Stratos LVS Extension could be used for integrating LVS load balancer with Apache Stratos. Please follow
below steps to proceed with the installation:

1. Install keepalived and ipvsadm:
   ```
   apt-get install keepalived ipvsadm
   ```

2. Open <lvs-extension-home>/bin/lvs-extension.sh file in a text editor and update following system properties:
   ```
   # Keepalived configuration file location:
   -Dconf.file.path=/etc/keepalived/keepalived.conf

   # Enable/disable cep statistics publisher:
   -Dcep.stats.publisher.enabled=false

   # If cep statistics publisher is enabled define the following properties:
   -Dthrift.receiver.ip=127.0.0.1
   -Dthrift.receiver.port=7615
   -Dnetwork.partition.id=network-partition-1

   # LVS server Virtual IP set for services
   -Dlvs.service.virtualip.set=tomcat2|192.168.56.40,tomcat1|192.168.56.41,tomcat|192.168.56.40
   # Server state (MASTER|BACKUP)
   -Dserver.state=MASTER

   ```

4. Open <lvs-extension-home>/conf/jndi.properties file in a text editor and update message broker information:
   ```
   java.naming.provider.url=tcp://localhost:61616
   ```

5. Run <lvs-extension-home>/bin/lvs-extension.sh as the root user.

