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
#   STRATOS_CLI_HOME   Home of Stratos CLI Tool
#
#   STRATOS_URL        The URL of the Stratos Controller

if [ -z $STRATOS_CLI_HOME ] ; then
STRATOS_CLI_HOME="$PWD"
fi

java -jar $STRATOS_CLI_HOME/org.apache.stratos.cli-3.0.0-SNAPSHOT-Tool.jar $*

