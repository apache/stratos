#!/bin/bash
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

echo "Removing json files created"
rm -f *.json 
echo "Removing payload directory"
rm -rf payload/
echo "Removing repo info soap request"
rm -f repoinforequest.xml
echo "Removing launch.params"
rm -f launch.params 
echo "Removing git.sh"
rm -f git.sh 
echo "Removing temporary git directory"
rm -rf /opt/temp_git 
echo "Removing content copied to the web server"
rm -rf /var/www/* /var/www/.git
echo "Removing cartridge agent logs"
rm -f /var/log/apache-stratos/*

