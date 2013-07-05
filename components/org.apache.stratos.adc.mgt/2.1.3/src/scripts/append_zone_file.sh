#!/bin/bash
echo "subdomain $1 and ip $2 added to $3"
appending_file=$3
subdomain=$1
ip=$2

#appending the zone file
echo $subdomain'\t'IN'\t'A'\t'$ip>> $appending_file

#increasing the count
for file in $appending_file;
do
  if [ -f $file ];
  then
    OLD=`egrep -ho "2010-9[0-9]*" $file`
    NEW=$(($OLD + 1))
    sed -i "s/$OLD/$NEW/g" $file
    echo "fixed $file" 
  fi
done


#reloading bind server
/etc/init.d/bind9 reload
