#!/bin/bash

tenant=""
cartridge=""

function help {
    echo "Usage: keygen <mandatory arguments>"
    echo "    Usage:"
    echo "    	  keygen <tenant> <cartridge>"
    echo "    eg:"
    echo "    	  keygen foo php"
    echo ""
}

function main {

if [[ (-z $tenant || -z $cartridge ) ]]; then
    help
    exit 1
fi

}

tenant=$1
cartridge=$2

if [[ (-n $tenant && -n $cartridge) ]]; then
	ssh-keygen -t rsa -N ''  -f /tmp/${tenant}-${cartridge}

fi

main
