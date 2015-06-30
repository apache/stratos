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

ENV JDK_VERSION 1.7.0_67
ENV JDK_TAR_FILENAME jdk-7u67-linux-x64.tar.gz
ENV TOMCAT_VERSION 7.0.59

# ----------------------
# Install prerequisites
# ----------------------
WORKDIR /opt
ADD packs/${JDK_TAR_FILENAME} /mnt/${JDK_TAR_FILENAME}
RUN mv /mnt/${JDK_TAR_FILENAME}/jdk${JDK_VERSION} /opt/jdk${JDK_VERSION}
ENV JAVA_HOME /opt/jdk${JDK_VERSION}

ENV CATALINA_HOME /opt/tomcat

# ----------------------
# Install Tomcat
# ----------------------
ADD packs/apache-tomcat-${TOMCAT_VERSION}.tar.gz /opt/apache-tomcat-${TOMCAT_VERSION}.tar.gz
RUN mv /opt/apache-tomcat-${TOMCAT_VERSION}.tar.gz/apache-tomcat-${TOMCAT_VERSION} /opt/tomcat && \
    rm -rf /opt/apache-tomcat-${TOMCAT_VERSION}.tar.gz

# -----------------------
# Add shell scripts
# -----------------------
ADD files/create-admin-user.sh /opt/create-admin-user.sh
RUN chmod +x /opt/create-admin-user.sh && \
    bash /opt/create-admin-user.sh

ADD files/env /tmp/env
RUN chmod +x /tmp/env && \
    sleep 1 && \
    /tmp/env ${JAVA_HOME} ${CATALINA_HOME}

#------------------------
# Copy Tomcat related PCA plugins
#-----------------------
ADD packs/plugins /mnt/plugins


EXPOSE 8080

# -----------------------
# Define entry point
# -----------------------
ENTRYPOINT /usr/local/bin/run | /usr/sbin/sshd -D
