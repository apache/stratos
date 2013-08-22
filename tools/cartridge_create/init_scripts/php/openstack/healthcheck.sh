#!/bin/bash

var=`nc -z localhost 80; echo $?`;
if [ $var -eq 0 ]
then
    echo "port 80 is available" > /dev/null 2>&1
else
    echo "port 80 is not available" > /dev/null 2>&1
    /etc/init.d/apache2 restart
fi
