#!/bin/bash

export PATH='/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin'

PASSWD=$1

service mysql start
echo $PASSWD> /tmp/udara
# Set mysql password
mysqladmin -uroot password "$PASSWD"

# Remove other users
mysql -uroot -p"$PASSWD" -Bse "DELETE from mysql.user WHERE password=''"

# Set root user with remote access
mysql -uroot -p"$PASSWD" -Bse "CREATE USER 'root'@'%' IDENTIFIED BY '${PASSWD}'"
