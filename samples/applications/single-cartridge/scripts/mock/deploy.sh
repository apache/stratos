#!/bin/sh

iaas="mock"
script_path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
common_folder="${script_path}/../common"

bash ${common_folder}/deploy.sh ${iaas}
