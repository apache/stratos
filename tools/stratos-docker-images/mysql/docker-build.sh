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

# copy stratos.mysql

# NOTE: mysql container uses--init-file option which does not like comments 
# and likes each command on its own line

cp -f $STRATOS_SOURCE/tools/stratos-installer/resources/mysql.sql files/mysql.tmp.0

# strip singleline comments
grep -v '^--.*$' files/mysql.tmp.0 > files/mysql.tmp.1

# strip multiline comments
perl -0777 -pe 's{/\*.*?\*/}{}gs' files/mysql.tmp.1 > files/mysql.sql

# remove newlines
sed -i -e ':a;N;$!ba;s/\n/ /g' files/mysql.sql

# replace ; with ;\n
sed -i -e 's/;/;\n/g' files/mysql.sql

rm files/mysql.tmp.*

### build docker

docker build -t=apachestratos/mysql:$VERSION .
