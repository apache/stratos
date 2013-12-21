#!/bin/bash

read -p "Please confirm that you want to clean this instance [y/n] " answer
if [[ $answer != y ]] ; then
    exit 1
fi

echo 'Stopping all java processes'
killall java
echo "Removing payload directory"
rm -rf payload/
echo "Removing launch.params"
rm -f launch.params 
echo "Removing content copied to the web server"
rm -rf /var/www/* /var/www/.git
echo "Removing cartridge agent logs"
rm -f /var/log/apache-stratos/*
echo "Removing load balancer logs"
rm load-balancer/nohup.out
echo "Cleaning completed"
