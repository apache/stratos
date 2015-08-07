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
# Apache Stratos HAProxy Extension

Apache Stratos HAProxy extension is a load balancer extension for HAProxy. It is an executable program
which can manage the life-cycle of a HAProxy instance according to the topology, composite application model,
tenant application signups and domain mapping information received from Stratos via the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure and start an instance of HAProxy.
3. Listen to topology, application, application signup, domain mapping events.
4. Reload HAProxy instance with the new topology configuration.
5. Periodically publish statistics to Complex Event Processor (CEP).

## Installation
Please refer INSTALL.md for information on the installation process.

Thanks to Vaadin for HAProxyController implementation:
https://vaadin.com/license
http://dev.vaadin.com/browser/svn/incubator/Arvue/ArvueMaster/src/org/vaadin/arvue/arvuemaster/HAProxyController.java

