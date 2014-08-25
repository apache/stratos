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

FROM    ubuntu:14.04

RUN apt-get update
RUN apt-get install -y bind9

COPY zone_template /etc/bind/zone_template

# disable reverse address lookups - this may not be possible without ISP delegating the reverse done
#COPY inaddr_template /etc/bind/inaddr_template

EXPOSE 53/udp

ADD run /usr/local/bin/run
RUN chmod +x /usr/local/bin/run

CMD ["/usr/local/bin/run"]
