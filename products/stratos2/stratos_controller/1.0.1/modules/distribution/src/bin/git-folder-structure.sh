#!/bin/bash

tenant=""
cartridge=""
ads_git_url="localhost"

function help {
    echo "Usage:git-folder-structure  <mandatory arguments>"
    echo "    Usage:"
    echo "    	  git-folder-structure <tenant> <cartridge> [webapp=readme file description with space replace with #] "
    echo "    eg:"
    echo "    	  git-folder-structure tenant1 as webapp=copy#war#files#here"
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
	cd /tmp/
	rm -fr ${tenant}/${cartridge}
	git clone git@localhost:${tenant}/${cartridge}
	cd ${cartridge}
	git pull origin master
	shift
	shift
	for IN in "$@"; do
		IFS='=' read -ra ADDR <<< "$IN"
		mkdir -p ${ADDR[0]}
		echo ${ADDR[1]} | sed -e 's/#/ /g' > ${ADDR[0]}/README.txt
		git add ${ADDR[0]}
		git commit -a -m 'Folder structure commit'
		git push origin master
	done
	rm -fr ${cartridge}	
fi	

main
