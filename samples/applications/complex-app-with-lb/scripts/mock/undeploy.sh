#!/bin/bash

current_folder="$( cd -P "$( dirname "$SOURCE" )" && pwd )/`dirname $0`"
common_folder="${current_folder}/../common"

pushd ${common_folder}
bash undeploy.sh
popd