#!/bin/bash

iaas="mock"
common_folder=`cd "${script_path}/../common"; pwd`

bash ${common_folder}/update-network-partition.sh ${iaas}
