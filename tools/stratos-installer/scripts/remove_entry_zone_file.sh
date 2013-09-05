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
zone_file=$2
subdomain=$1

# check the file
if [ -f {$zone_file} ]; then
	echo "Error: zone does not exist"
	exit 1
fi
echo "File $zone_file exists"

# find entry to delete
entry=$(grep $subdomain $zone_file)
#sed "/$entry/d" $zone_file 

sed "/$entry/d" $zone_file >tmp
mv tmp $zone_file

echo "entry to delete  $entry"


# get serial number
serial=$(grep 'Serial' $zone_file | awk '{print $1}')
echo "Serial number " $serial
# get serial number's date
serialdate=$(echo $serial | cut -b 1-8)
# get today's date in same style
date=$(date +%Y%m%d)


#Serial number's date
serialdate=$(echo $serial | cut -b 1-8)
echo "serial date" $serialdate
# get today's date in same style
date=$(date +%Y%m%d)
echo "Now date" $date

# compare date and serial date
if [ $serialdate = $date ]
	then
		# if equal, just add 1
		newserial=$(expr $serial + 1)
		echo "same date"
	else
		# if not equal, make a new one and add 00
		newserial=$(echo $date"00")
fi

echo "Adding subdomain $1 and ip $2 to $3"
sed -i "s/.*Serial.*/ \t\t\t\t$newserial ; Serial./" $zone_file



#reloading bind server
/etc/init.d/bind9 reload
