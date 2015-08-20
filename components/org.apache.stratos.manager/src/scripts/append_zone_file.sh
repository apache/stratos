#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
echo "subdomain $1 and ip $2 added to $3"
appending_file=$3
subdomain=$1
ip=$2

#appending the zone file
echo $subdomain'\t'IN'\t'A'\t'$ip>> $appending_file

#increasing the count
for file in $appending_file;
do
  if [ -f $file ];
  then
    OLD=`egrep -ho "2010-9[0-9]*" $file`
    NEW=$(($OLD + 1))
    sed -i "s/$OLD/$NEW/g" $file
    echo "fixed $file" 
  fi
done


#reloading bind server
/etc/init.d/bind9 reload
