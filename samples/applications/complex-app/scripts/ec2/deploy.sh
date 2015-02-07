#!/bin/bash

iaas="ec2"

prgdir=`dirname "$0"`
script_path=`cd "$prgdir"; pwd`
common_folder=`cd "${script_path}/../common"; pwd`

bash ${common_folder}/deploy.sh ${iaas}
