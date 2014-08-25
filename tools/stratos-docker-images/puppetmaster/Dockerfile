# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ----------------------------------------------------------------------------

FROM ubuntu:14.04

RUN apt-get update -qq 
RUN apt-get install -y wget

RUN wget http://apt.puppetlabs.com/puppetlabs-release-precise.deb
RUN dpkg -i puppetlabs-release-precise.deb
RUN apt-get update -qq 
RUN apt-get install -y puppetmaster

RUN mkdir -p /etc/puppet/modules/agent/files
RUN mkdir -p /etc/puppet/modules/java/files
RUN mkdir -p /etc/puppet/modules/tomcat/files

COPY files/manifests /etc/puppet/manifests
COPY files/modules /etc/puppet/modules

ADD files/jdk-7u51-linux-x64.tar.gz /etc/puppet/modules/java/files/
ADD files/apache-tomcat-7.0.52.tar.gz /etc/puppet/modules/tomcat/files/

ADD files/agentfiles.tgz /tmp/

RUN mv /tmp/apache-stratos-cartridge-agent-*.zip /etc/puppet/modules/agent/files/
RUN mv /tmp/apache-stratos-load-balancer-*.zip /etc/puppet/modules/agent/files/

ADD run /usr/local/bin/run
RUN chmod +x /usr/local/bin/run

CMD ["/usr/local/bin/run"]

