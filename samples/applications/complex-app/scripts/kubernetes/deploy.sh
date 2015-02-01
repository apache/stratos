#!/bin/bash

iaas="kubernetes"
current_folder="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
common_folder="${current_folder}/../common"

pushd ${common_folder}
bash deploy.sh ${iaas}
popd