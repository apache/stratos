#!/bin/bash

iaas="openstack"

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`

bash ${common_folder}/deploy.sh ${iaas}