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

echo "Starting haproxy extension..."
lib_path=./../lib/
class_path=`echo ../lib/*.jar | tr ' ' ':'`
properties="-Djndi.properties.dir=./../conf -Dexecutable.file.path=haproxy -Dtemplates.path=./../templates -Dtemplates.name=haproxy.cfg.template -Dconf.file.path=/tmp/haproxy.cfg"

java -cp "$class_path" $properties org.apache.stratos.haproxy.extension.Main -Dp1=sample_value $*
