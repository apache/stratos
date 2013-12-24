#!/bin/sh
# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------
#  This script could be used to deploy cartridge definitions via the rest-api.
#

# Parameter values
stratos_manager_host=
stratos_manager_https_port=
username=admin
password=admin
json_file="/path/to/cartridge.json"

# Send request
curl -X POST -H "Content-Type: application/json" -d @'${json_file}' -k -v -u ${username}:${password} https://${stratos_manager_host}:${stratos_manager_https_port}/stratos/admin/cartridge/definition