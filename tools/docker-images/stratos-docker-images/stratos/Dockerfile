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

RUN apt-get update && \
    apt-get upgrade tzdata -y && \
    apt-get install openjdk-7-jre-headless -y && \
    apt-get clean && \
    rm -rf /var/cache/apt/*


#################
# Unpack Stratos 
#################

ADD /files/apache-stratos /opt/apache-stratos/

ADD /files/stratos-installer/config/all/repository/conf/activemq/jndi.properties /opt/apache-stratos/repository/conf/

####################
# Get ActiveMQ libs
####################

#ADD /files/apache-activemq-lib /opt/apache-stratos/repository/components/lib/ 

###########
# CC Setup
###########

ADD /files/stratos-installer/config/all/repository/conf/cloud-controller.xml /opt/apache-stratos/repository/conf/

###########
# AS Setup
###########

ADD  /files/stratos-installer/config/all/repository/conf/autoscaler.xml /opt/apache-stratos/repository/conf/

###########
# SM Setup
###########

ADD /files/stratos-installer/config/all/repository/conf/cartridge-config.properties /opt/apache-stratos/repository/conf/
ADD /files/stratos-installer/config/all/repository/conf/datasources/master-datasources.xml /opt/apache-stratos/repository/conf/datasources/
ADD /files/mysql-connector-java-5.1.29.jar /opt/apache-stratos/repository/components/lib/

############
# CEP Setup
############

#ADD /files/extensions/cep/artifacts/stream_definitions/stream-manager-config.xml /opt/apache-stratos/repository/conf/

###################
# Setup run script
###################

ADD run /usr/local/bin/run
RUN chmod +x /usr/local/bin/run

CMD ["/usr/local/bin/run"]
