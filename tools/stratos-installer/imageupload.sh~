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
#
#  This script is for publishing vm images to the cloud environment (IaaS).
# ----------------------------------------------------------------------------

TMPAREA=/tmp/__upload

# Process Command Line
while getopts a:p:t:C:x:y:z:n:w: opts
do
  case $opts in
    a)
        ADMIN=${OPTARG}
        ;;
    p)
        PASSWORD=${OPTARG}
        ;;
    t)
        TENANT=${OPTARG}
        ;;
    C)
        ENDPOINT=${OPTARG}
        ;;
    x)
        ARCH=${OPTARG}
        ;;
    y)
        DISTRO=${OPTARG}
        ;;
    z)
        ARCHFILE=${OPTARG}
        ;;
    n)
        IMAGENAME=${OPTARG}
        ;;
    w)
        VERSION=${OPTARG}
        ;;
    *)
        echo "Syntax: $(basename $0) -u USER -p KEYSTONE -t TENANT -C CONTROLLER_IP -x ARCH -y DISTRO -w VERSION -z ARCHFILE -n IMAGENAME"
        exit 1
        ;;
  esac
done

IMAGE=`basename $ARCHFILE`
EXTENSION="${IMAGE##*.}"

# You must supply the API endpoint
if [[ ! $ENDPOINT ]]
then
        echo "Syntax: $(basename $0) -a admin -p PASSWORD -t TENANT -C CONTROLLER_IP"
        exit 1
fi



mkdir -p ${TMPAREA}
if [ ! -f ${TMPAREA}/${IMAGE} ]
then
        cp ${ARCHFILE} ${TMPAREA}/${IMAGE}
fi

if [ -f ${TMPAREA}/${IMAGE} ]
then
	cd ${TMPAREA}
    if [[ ${EXTENSION} == "gz" || ${EXTENSION} == "tgz" ]]; then
	    tar zxf ${IMAGE}
	    DISTRO_IMAGE=$(ls *.img)
	elif [[ ${EXTENSION} == "img" ]]; then
	    DISTRO_IMAGE=${IMAGE}
    fi

	AMI=$(glance -I ${ADMIN} -K ${PASSWORD} -T ${TENANT} -N http://${ENDPOINT}:5000/v2.0 add name="${IMAGENAME}" disk_format=ami container_format=ami distro="${DISTRO} ${VERSION}" kernel_id=${KERNEL} is_public=true < ${DISTRO_IMAGE} | awk '/ ID/ { print $6 }')

	echo "${DISTRO} ${VERSION} ${ARCH} now available in Glance (${AMI})"

	rm -f /tmp/__upload/*{.img,-vmlinuz-virtual,loader,floppy}
else
	echo "Tarball not found!"
fi
