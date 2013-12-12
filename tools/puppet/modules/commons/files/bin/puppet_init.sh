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

WHICH=`/usr/bin/which which` 
PUPPETD=`${WHICH} puppetd`
GREP=`${WHICH} grep`
PS=`${WHICH} ps`
SLEEP=`${WHICH} sleep`

if [ $# -eq 1 ]; then 
	SERVICES=$@; COMMAND="${PUPPETD} -vt --tags ${SERVICES}"
elif [ $# -eq 0 ]; then
	COMMAND="${PUPPETD} -vt"
fi

read_master() {
	local COUNT=0
	[ ${COUNT} == 10 ] && exit 1 || true
	${COMMAND}
	${PS} -auwx | ${GREP} java | ${GREP} wso2
#	[ $? == 0 ] && true || (read_master ; COUNT=`expr ${COUNT} + 1`)
}

echo "Asia/Colombo" > /etc/timezone
dpkg-reconfigure --frontend noninteractive tzdata

${PUPPETD} --enable

read_master

${PUPPETD} --disable

exit 0

