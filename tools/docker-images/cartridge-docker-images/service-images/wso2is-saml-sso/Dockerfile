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

FROM stratos/base-image:4.1.0
MAINTAINER dev@stratos.apache.org

ENV DEBIAN_FRONTEND noninteractive
ENV JDK_VERSION 1.7.0_60
ENV JDK_TAR_FILENAME jdk-7u60-linux-x64.tar.gz
ENV WSO2_IS_VERSION 5.0.0

# ----------------------
# Install prerequisites
# ----------------------
WORKDIR /opt
ADD packs/${JDK_TAR_FILENAME} /mnt/${JDK_TAR_FILENAME}
RUN mv /mnt/${JDK_TAR_FILENAME}/jdk${JDK_VERSION} /opt/jdk${JDK_VERSION}

ENV JAVA_HOME /opt/jdk${JDK_VERSION}

# -----------------------------
# Install WSO2 Identity Server
# -----------------------------
ADD packs/wso2is-${WSO2_IS_VERSION}.zip /opt/wso2is-${WSO2_IS_VERSION}.zip
RUN unzip /opt/wso2is-${WSO2_IS_VERSION}.zip -d /opt/ && \
    rm /opt/wso2is-${WSO2_IS_VERSION}.zip
ENV CARBON_HOME /opt/wso2is-${WSO2_IS_VERSION}

ADD files/env /tmp/env
RUN chmod +x /tmp/env && \
    sleep 1 && \
    /tmp/env ${JAVA_HOME} ${CARBON_HOME}

ADD files/carbon.xml ${CARBON_HOME}/repository/conf/carbon.xml
ADD files/catalina-server.xml ${CARBON_HOME}/repository/conf/tomcat/catalina-server.xml

#---------------------------------
# Copy Tomcat related PCA plugins
#---------------------------------
ADD packs/plugins /mnt/plugins


EXPOSE 9443

# -----------------------
# Define entry point
# -----------------------
ENTRYPOINT /usr/local/bin/run | /usr/sbin/sshd -D
