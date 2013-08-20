#!/bin/bash

user="ubuntu"
instance_ip=""
app_path=""
cartridge_private_key=""

function help {
    echo "Usage: update-instance <mandatory arguments>"
    echo "    Usage:"
    echo "    	  update-instance <instance_ip> <app path> <cartridge_private_key>"
    echo "    eg:"
    echo "    	  update-instance 172.17.1.1 /var/www/myapp /tmp/foo-php"
    echo ""
}

function main {

if [[ (-z $instance_ip || -z $app_path || -z $cartridge_private_key ) ]]; then
    help
    exit 1
fi

}

instance_ip=$1
app_path=$2
cartridge_private_key=$3

if [[ (-n $instance_ip && -n $app_path && -n $cartridge_private_key ) ]]; then
    ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} sudo cd $app_path; sudo git pull
fi

main
