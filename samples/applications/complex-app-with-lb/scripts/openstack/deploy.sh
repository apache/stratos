#!/bin/bash

iaas="openstack"
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
common_folder="${script_path}/../common"

pushd ${common_folder}
bash deploy.sh ${iaas}
popd