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
#  Main Script for the Apache Stratos CLI Tool
#
#  Environment Variable Prerequisites
#
#   STRATOS_URL: The URL of the Stratos Manager
#   Example: https://<hostname>:9443
# ----------------------------------------------------------------------------

set -e

if [ -z "$STRATOS_URL" ]; then
    echo "STRATOS_URL environment variable is not set"
fi

script_path="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
lib_path=${script_path}/../lib/
class_path=`echo ${lib_path}/*.jar | tr ' ' ':'`

properties="-Dlog4j.configuration=file://${script_path}/../conf/log4j.properties"
# Uncomment following line to enable remote debugging
#debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

java -cp "${class_path}" ${properties} ${debug} org.apache.stratos.cli.Main $*
