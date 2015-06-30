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

# ----------------
# Install PHP
# ----------------
RUN apt-get install -y apache2 php5 zip stress
RUN rm -f /etc/apache2/sites-enabled/000-default.conf
ADD files/000-default.conf /etc/apache2/sites-enabled/000-default.conf

#------------------------
# Copy PHP related PCA plugins
#-----------------------
ADD packs/plugins /mnt/plugins

EXPOSE 80

# ----------------
# Entrypoint
# ----------------
ENTRYPOINT /usr/local/bin/run | /usr/sbin/sshd -D
