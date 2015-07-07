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

FROM debian:7.7
MAINTAINER dev@stratos.apache.org

# ------------------
# Setup ssh server
# ------------------
WORKDIR /opt/
RUN apt-get update && apt-get install -y openssh-server git
RUN mkdir -p /var/run/sshd
RUN echo 'root:stratos' | chpasswd
RUN sed -i "s/PermitRootLogin without-password/#PermitRootLogin without-password/" /etc/ssh/sshd_config
EXPOSE 22

# ----------------------
# Install dependencies
# ----------------------
RUN apt-get install -y git python python-pip python-dev gcc zip
RUN pip install paho-mqtt
RUN pip install psutil
RUN pip install pexpect
RUN pip install pycrypto
RUN pip install gitpython
RUN pip install yapsy

# -------------------------
# Install cartridge agent 
# -------------------------
WORKDIR /mnt/

ADD packs/apache-stratos-python-cartridge-agent-4.1.0.zip /mnt/apache-stratos-python-cartridge-agent-4.1.0.zip
RUN unzip -q /mnt/apache-stratos-python-cartridge-agent-4.1.0.zip -d /mnt/
RUN rm /mnt/apache-stratos-python-cartridge-agent-4.1.0.zip

RUN mkdir -p /mnt/apache-stratos-python-cartridge-agent-4.1.0/payload

RUN chmod +x /mnt/apache-stratos-python-cartridge-agent-4.1.0/extensions/bash/*
RUN mkdir -p /var/log/apache-stratos/
RUN touch /var/log/apache-stratos/cartridge-agent-extensions.log

# -----------------------
# Setup startup scripts
# -----------------------
ADD files/run /usr/local/bin/run
RUN chmod +x /usr/local/bin/run
