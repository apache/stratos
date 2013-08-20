#!/bin/bash

user="ubuntu"
instance_ip=""
cartridge_private_key=""
password=""


function help {
    echo "Usage: set-mysql-password <mandatory arguments>"
    echo "    Usage:"
    echo "    	  set-mysql-password <instance ip> <cartridge private key> <password>"
    echo "    eg:"
    echo "    	  set-mysql-password 172.17.1.2 /tmp/foo-php qazxsw"
    echo ""
}

function main {

if [[ (-z $password || -z $instance_ip) ]]; then
    help
    exit 1
fi

}

instance_ip=$1
cartridge_private_key=$2
password=$3

if [[ (-n $password && -n $instance_ip) ]]; then
	ssh -o "BatchMode yes" -i ${cartridge_private_key} ${user}@${instance_ip} mysqladmin -u root password "${password}"
fi

main
