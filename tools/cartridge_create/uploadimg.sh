#!/bin/bash
ip=$1
imagename=$2
userrc=$3
mysqlpass=$4
source ./$userrc
ret=`mysql -u root -p$mysqlpass nova -e "select i.uuid from instances i, fixed_ips x, floating_ips f where i.id=x.instance_id and x.id=f.fixed_ip_id and f.address='$ip' and i.vm_state='ACTIVE'"`
tok_str=(`echo $ret | tr '.' ' '`)
id=${tok_str[1]}
echo $id > /tmp/test
nova image-create $id $imagename
