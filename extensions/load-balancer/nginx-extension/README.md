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
# Apache Stratos Nginx Extension

Apache Stratos Nginx extension is a load balancer extension for Nginx. It is an executable program
which can manage the life-cycle of a Nginx instance according to the topology, composite application model,
tenant application signups and domain mapping information received from Stratos via the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure and start an instance of Nginx.
3. Listen to topology, application, application signup, domain mapping events.
4. Reload Nginx instance with the new topology configuration.
5. Periodically publish statistics to Complex Event Processor (CEP).

## Statistics publishing
Set cep.stats.publisher.enabled to true in nginx-extension.sh file to enable statistics publishing. Please note that
Nginx must be compiled with HttpStubStatusModule module to read statistics. Execute the following command to make
 sure that HttpStubStatusModule module is installed:
```
nginx -V 2>&1 | grep -o with-http_stub_status_module
```
If HttpStubStatusModule is installed the following output will be given:
```
with-http_stub_status_module
```

## Installation
Please refer INSTALL.md for information on the installation process.
