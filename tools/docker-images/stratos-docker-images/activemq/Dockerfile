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
#
#  Server configuration script for Apache Stratos
# ----------------------------------------------------------------------------

FROM ubuntu:14.04

RUN apt-get update && \
    apt-get upgrade tzdata -y && \
    apt-get install openjdk-7-jre-headless curl -y && \
    apt-get clean && \
    rm -rf /var/cache/apt/*


RUN curl http://www.mirrorservice.org/sites/ftp.apache.org/activemq/5.10.0/apache-activemq-5.10.0-bin.tar.gz | tar -zx

EXPOSE 61616 8161

CMD java -Xms1G -Xmx1G -Djava.util.logging.config.file=logging.properties -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote -Djava.io.tmpdir=apache-activemq-5.10.0/tmp -Dactivemq.classpath=apache-activemq-5.10.0/conf -Dactivemq.home=apache-activemq-5.10.0 -Dactivemq.base=apache-activemq-5.10.0 -Dactivemq.conf=apache-activemq-5.10.0/conf -Dactivemq.data=apache-activemq-5.10.0/data -jar apache-activemq-5.10.0/bin/activemq.jar start
