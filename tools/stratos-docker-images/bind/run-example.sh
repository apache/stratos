#!/bin/bash
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

# DOMAIN is the domain name for the stratos environment
DOMAIN=example.com

# IP_ADDR is the IP address of the host running the DNS docker image
IP_ADDR=192.168.56.5

# UPDATE_ADDR_LIST list of addresses that can dynamically update the DNS server # see http://www.zytrax.com/books/dns/ch7/address_match_list.html
UPDATE_ADDR_LIST=any


BIND_ID=$(docker run -d -p 53:53/udp -e "DOMAIN=$DOMAIN" -e "IP_ADDR=$IP_ADDR" -e "UPDATE_ADDR_LIST=$UPDATE_ADDR_LIST" apachestratos/bind)
BIND_IP_ADDR=$(docker inspect --format '{{ .NetworkSettings.Gateway }}' $BIND_ID)

