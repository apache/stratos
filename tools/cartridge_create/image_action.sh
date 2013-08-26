#!/bin/bash

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


# Die on any error:
set -e

work_dir=""
action=""
image_template=""
image_root=""
software=""
image_type=""
loopdevice="/dev/loop1"
nbddevice="/dev/nbd3"
nbdmount="/dev/nbd3p1"

function image_validate {
    if [ -z $action ]; then
        echo "usage: image_action.sh action=<mount|create|unmount> <options>"
        exit 1
    fi

    if [[ !($action == "mount") && !($action == "create") && !($action == "unmount") ]]; then
        echo "usage: image_action.sh action=<mount|create|unmount> <options>"
        exit 1
    fi

    if [[ (-n $action) && ($action == "create") && ( -z $image_template || -z $image_root ) ]]; then
        echo "usage: image_action.sh action=create template=<template> image-root=<image_root>"
        exit 1
    fi

    if [[ (-n $action) && ($action == "unmount") && (-z $image_template || -z $image_root) ]]; then
        echo "usage: image_action.sh action=unmount image-id=<image_template> image-root=<image_root>"
        exit 1
    fi
    
    if [[ (-n $action) && ($action == "mount") && (-z $image_template || -z $image_root || -z $image_image) ]]; then
        echo "usage: image_action.sh action=mount image-template=<image_template> image-root=<image_root> image-image=<image_image>"
        exit 1
    fi
    
}

for var in $@; do
    set -- `echo $var | tr '=' ' '`
    key=$1
    value=$2

    if [ $key = "action" ]; then
        action="$value"
        echo "action:" $action
    fi

    if [ $key = "image-root" ]; then
        image_root="$value"
        echo "image root:" $image_root
    fi
    
    if [ $key = "image-template" ]; then
        image_template="$value"
        echo "template:" $image_template
    fi
    
    if [ $key = "image-image" ]; then
        image_image="$value"
        echo "image-image:" $image_image
    fi
    
    if [ $key = "software" ]; then
        software="$value"
        echo "software:" $software
    fi
    
    if [ $key = "image-type" ]; then
        image_type="$value"
        echo "image_type:" $image_type
    fi

done

image_validate

work_dir="$image_root/$image_template"
if [[ (-n $action) && ($action == "unmount") ]]; then
    echo "# unmount $image_template"
    if [ -d $work_dir/$image_template ]; then
        if [ $image_type == "qcow2" ]; then
            umount $work_dir/$image_template
            qemu-nbd -d $nbddevice
            rmmod nbd
        else
            echo 'this action unmount image!'
            umount $work_dir/$image_template/dev
            umount $work_dir/$image_template/proc
            umount $work_dir/$image_template/sys
            umount $work_dir/$image_template
            if [ -d $work_dir/$image_template/dev ]; then
                umount -l $work_dir/$image_template
            fi
            losetup -d $loopdevice
        fi
    fi
    exit 0
fi

if [[ (-n $action) && ($action == "mount") ]]; then
    echo "# mount $image_template "
    if [ ! -d "$work_dir/$image_template" ]; then
        mkdir -p $work_dir/$image_template
    fi
    #cp -f $image_image $work_dir/
    if [ $image_type == "qcow2" ]; then
        modprobe nbd max_part=8
        qemu-nbd -c $nbddevice $image_image
        mount $nbdmount $work_dir/$image_template
    else
        #loopdevice=`losetup -f`
        echo "Available loop device is $loopdevice"
        losetup $loopdevice $image_image
        #kpartx -av $loopdevice
        mount $loopdevice $work_dir/$image_template 
        mount -o bind /dev $work_dir/$image_template/dev
        mount -o bind /proc $work_dir/$image_template/proc
        mount -o bind /sys $work_dir/$image_template/sys
    fi
    exit 0
fi

# Where to create newly-created LXC container rootfs filesystems, fstabs, and confs:
export BASEDIR=$work_dir

# This is a list of semi-colon separated files/directories that will be needed by templates.
export SOFTWARE="$software"
export ROOTFS=$work_dir/$image_template
export TEMPLATE=$image_template

if [ -z "$TEMPLATE" ]; then
	TEMPLATE="default"
fi

./configure_software $ROOTFS $TEMPLATE
echo

exit

