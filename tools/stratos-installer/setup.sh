#!/bin/bash
# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ----------------------------------------------------------------------------
#
#  Server configuration script for Apache Stratos
# ----------------------------------------------------------------------------

# Die on any error:
set -e

SLEEP=60

source "./conf/setup.conf"
export LOG=$log_path/stratos-setup.log

mb="false"
cc="false"
lb="false"
as="false"
sc="false"
cep="false"
product_list="mb;cc;cep;lb;as;sc"
enable_internal_git=false

function help {
    echo ""
    echo "Usage:"
    echo "setup.sh -u <host username> -p \"<product list>\""
    echo "product list : [mb, cc, lb, as, sm, cep]"
    echo "Example:"
    echo "sudo ./setup.sh -p \"cc llb\""
    echo "sudo ./setup.sh -p \"all\""
    echo ""
    echo "-u: <host username> The login user of the host."
    echo "-p: <product list> Apache Stratos products to be installed on this node. Provide one or more names of the servers."
    echo "    The available servers are cc, lb, as, sc or all. 'all' means you need to setup all servers in this machine. Default is all"
    echo "-g: <enable_internal_git> true|false Whether enable internal git repo for Stratos2. Default is false"
    echo ""
}

while getopts u:p:g: opts
do
  case $opts in
    p)
        product_list=${OPTARG}
        ;;
    g)
        enable_internal_git=${OPTARG}
        ;;
    \?)
        help
        exit 1
        ;;
  esac
done


arr=$(echo $product_list | tr " " "\n")

for x in $arr
do
    if [[ $x = "mb" ]]; then
        mb="true"
    fi
    if [[ $x = "cc" ]]; then
        cc="true"
    fi
    if [[ $x = "cep" ]]; then
        cep="true"
    fi
    if [[ $x = "lb" ]]; then
        lb="true"
    fi
    if [[ $x = "as" ]]; then
        as="true"
    fi
    if [[ $x = "sc" ]]; then
        sc="true"
    fi
    if [[ $x = "all" ]]; then
        mb="true"
        cc="true"
        lb="true"
        as="true"
        sc="true"
    fi
done
product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi

function helpsetup {
    echo ""
    echo "Set up the environment variables correctly in conf/setup.conf"
    echo ""
}

echo "user provided in conf/setup.conf is $host_user. If you want to provide some other user name please specify it at the prompt."
echo "If you want to continue with $host_user just press enter to continue"
read username
if [[ $username != "" ]]; then
    host_user=$username
fi
user=`id $host_user`
if [[ $? = 1 ]]; then
    echo "User $host_user does not exist. The system will create it."
    adduser --home /home/$host_user $host_user
fi

echo "StrictHostKeyChecking no" > /home/$host_user/.ssh/config
chmod 600 /home/$host_user/.ssh/config
chown $host_user:$host_user /home/$host_user/.ssh/config
export $enable_internal_git
export $host_user
export hostname=`hostname -f`

function setup_validate {    
    if [[ -z $hostname ]]; then
        echo "Set up the hostname of the node"
        exit 1
    fi

    if [[ -z $mb_hostname ]]; then
        mb_hostname=$hostname
    fi
    if [[ -z $userstore_db_hostname ]]; then
        userstore_db_hostname=""
    fi
    if [[ -z $sc_hostname ]]; then
        sc_hostname=$hostname
    fi
    if [[ -z $stratos_foundation_db_hostname ]]; then
        stratos_foundation_db_hostname=$hostname
    fi
    if [[ -z $as_hostname ]]; then
        as_hostname=$hostname
    fi
    if [[ -z $cc_hostname ]]; then
        cc_hostname=$hostname
    fi
    if [[ -z $git_hostname ]]; then
        git_hostname=$hostname
    fi
    if [[ -z $nova_controller_hostname ]]; then
        nova_controller_hostname=$hostname
    fi
    if [[ -z $bam_hostname ]]; then
        bam_hostname=$hostname
    fi
    if [[ -z $lb_hostname ]]; then
        lb_hostname=$hostname
    fi
    if [[ -z $cep_hostname ]]; then
        cep_hostname=$hostname
    fi

    if [[ ( -z $hostip ) ]]; then
        hostip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z $hostip ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ( -z $mb_ip ) ]]; then
        mb_ip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z mb_ip ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ( -z $cep_ip ) ]]; then
        cep_ip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z cep_ip ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ( -z $as_ip ) ]]; then
        as_ip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z $as_ip ) ]]; then
            helpsetup
            exit 1
        fi
    fi


    if [[ -z $git_ip ]]; then
        git_ip=$hostip
    fi

    if [[ $mb = "true" ]]; then
        if [[ ( -z $hostname || -z $mb_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $sc = "true" ]]; then
        if [[ $enable_internal_git = "true" ]]; then
            if [[ -z $git_user ]]; then
                echo "Please specify the git user, because it will be needed to create an internal git repo"
            fi
            if [[ -z $axis2c_path ]]; then
                echo "Please specify the path to Axis2/C binary, because it will be needed to create an internal git repo"
            fi

            echo "$hostip    git.$stratos_domain" >> /etc/hosts
        fi
        if [[ ( -z $email|| -z $stratos_foundation_db_user || -z $stratos_foundation_db_pass || -z $hostname
            || -z $sc_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $cc = "true" ]]; then
        if [[ ( -z $hostname || -z $cc_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $cep = "true" ]]; then
        if [[ ( -z $hostname || -z $cep_path || ! -f $cep_extension_jar ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $lb = "true" ]]; then
        if [[ ( -z $hostname || -z $lb_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $as = "true" ]]; then
        if [[ ( -z $hostname || -z $as_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ! -f $mysql_connector_jar ]]; then
        echo "Please copy the mysql connector jar into the same folder as this command(stratos2 release pack folder) and update conf/setup.conf file"
        exit 1
    fi

    if [[ ! -d $JAVA_HOME ]]; then
        echo "Please set the JAVA_HOME environment variable for the running user"
        exit 1
    fi
    export JAVA_HOME=$JAVA_HOME

    if [[ $ec2_provider_enabled = "false" && $openstack_provider_enabled = "false" ]]; then
        echo "Please enable at least one of the IaaS providers in conf/setup.conf file"
        exit 1
    fi

    if [[ $openstack_provider_enabled = "true" ]]; then
        if [[ ( -z $openstack_identity || -z $openstack_credential || -z $openstack_jclouds_endpoint ) ]]; then
            echo "Please set openstack configuration information in conf/setup.conf file"
            exit 1
        fi
    fi

    if [[ $ec2_provider_enabled = "true" ]]; then
        if [[ ( -z $ec2_identity || -z $ec2_credential || -z $ec2_keypair_name ) ]]; then
            echo "Please set ec2 configuration information in conf/setup.conf file"
            exit 1
        fi
    fi
}

setup_validate

# Make sure the user is running as root.
if [ "$UID" -ne "0" ]; then
	echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
	exit 69
fi

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi

echo ""
echo "For all the questions asked while during executing the script please just press the enter button"
echo ""

if [[ $mb = "true" ]]; then
    if [[ ! -d $mb_path ]]; then
        unzip $mb_pack -d $stratos_path
    fi
fi
if [[ $sc = "true" ]]; then
    if [[ ! -d $resource_path ]]; then
        cp -rf ./resources $stratos_path
    fi

    if [[ ! -d $script_path ]]; then
        cp -rf ./scripts $stratos_path
    fi

    if [[ ! -d $sc_path ]]; then
        unzip $sc_pack -d $stratos_path
    fi
fi
if [[ $lb = "true" ]]; then
    if [[ ! -d $lb_path ]]; then
        unzip $lb_pack -d $stratos_path
    fi
fi
if [[ $cc = "true" ]]; then
    if [[ ! -d $cc_path ]]; then
        unzip $cc_pack -d $stratos_path
    fi
fi
if [[ $as = "true" ]]; then
    if [[ ! -d $as_path ]]; then
        unzip $as_pack -d $stratos_path
    fi
fi
if [[ $cep = "true" ]]; then
    if [[ ! -d $cep_path ]]; then
        unzip $cep_pack -d $stratos_path
    fi
fi

# ------------------------------------------------
# Setup MB
# ------------------------------------------------
if [[ $mb = "true" ]]; then
    echo "Setup MB" >> $LOG
    echo "Configuring the Message Broker"

    pushd $mb_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@<Offset>0</Offset>@<Offset>${mb_port_offset}</Offset>@g" > repository/conf/carbon.xml

    echo "End configuring the Message Broker"
    popd #mb_path
fi

# ------------------------------------------------
# Setup CEP
# ------------------------------------------------
if [[ $cep = "true" ]]; then
    echo "Setup CEP" >> $LOG
    echo "Configuring the Complex Event Processor"

    cp -f ./config/cep/repository/conf/jndi.properties $cep_path/repository/conf/
    cp -f $cep_extension_jar $cep_path/repository/components/lib/
    cp -f $cep_extension_path/artifacts/eventbuilders/*.xml $cep_path/repository/deployment/server/eventbuilders/
    cp -f $cep_extension_path/artifacts/inputeventadaptors/*.xml $cep_path/repository/deployment/server/inputeventadaptors/
    cp -f $cep_extension_path/artifacts/outputeventadaptors/*.xml $cep_path/repository/deployment/server/outputeventadaptors/
    cp -f $cep_extension_path/artifacts/executionplans/*.xml $cep_path/repository/deployment/server/executionplans/
    cp -f $cep_extension_path/artifacts/eventformatters/*.xml $cep_path/repository/deployment/server/eventformatters/

    pushd $cep_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@<Offset>0</Offset>@<Offset>${cep_port_offset}</Offset>@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/jndi.properties

    echo "In repository/conf/siddhi/siddhi.extension"
    cp -f repository/conf/siddhi/siddhi.extension repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.GradientFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.SecondDerivativeFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.FaultHandlingWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    mv -f repository/conf/siddhi/siddhi.extension.orig repository/conf/siddhi/siddhi.extension

    echo "End configuring the Complex Event Processor"
    popd #cep_path
fi


if [[ $sc = "true" ]]; then
    ##
#    mysql -u${userstore_db_user} -p${userstore_db_pass} -e "GRANT ALL PRIVILEGES ON *.* TO '${userstore_db_user}'@'%'   IDENTIFIED BY '${userstore_db_pass}' WITH GRANT OPTION;flush privileges;"


    # Setup SC
    #--------------------------------------------------
    echo "Setup SC" >> $LOG
    echo "Configuring SC"

    cp -f ./config/sc/repository/conf/cartridge-config.properties $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/carbon.xml $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/axis2/axis2.xml $sc_path/repository/conf/axis2
    cp -f ./config/sc/bin/stratos.sh $sc_path/bin/
    cp -fr ./config/sc/repository/resources/user-data/* $sc_path/repository/resources/user-data/
    cp -f ./config/sc/repository/conf/datasources/master-datasources.xml $sc_path/repository/conf/datasources/
    cp -f ./config/sc/repository/conf/datasources/stratos-datasources.xml $sc_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $sc_path/repository/components/lib/
    cp -f ./config/sc/repository/conf/log4j.properties  $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/etc/logging-config.xml  $sc_path/repository/conf/etc/
    pushd $sc_path

    echo "Set mb hostname and mb port in bin/stratos.sh." >> $LOG
    cp -f ./bin/stratos.sh bin/stratos.sh.orig
    cat bin/stratos.sh.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > bin/stratos.sh

    echo "Change CC hostname in repository/conf/cartridge-config.properties" >> $LOG

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@CC_HOSTNAME:CC_HTTPS_PORT@$cc_hostname:$cc_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@AGENT_HOSTNAME:AGENT_PORT@$agent_ip:$agent_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > repository/conf/cartridge-config.properties

    if [[ $enable_internal_git = "true" ]]; then
        cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
        cat repository/conf/cartridge-config.properties.orig | sed -e "s@GIT_IP@$git_ip@g" > repository/conf/cartridge-config.properties
    fi

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@SC_HOSTNAME:SC_HTTPS_PORT@$sc_ip:$sc_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_FOUNDATION_DB_HOSTNAME:STRATOS_FOUNDATION_DB_PORT@$stratos_foundation_db_hostname:$stratos_foundation_db_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_FOUNDATION_DB_USER@$stratos_foundation_db_user@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_FOUNDATION_DB_PASS@$stratos_foundation_db_pass@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_FOUNDATION_DB_SCHEMA@$stratos_foundation_db_schema@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@ELB_IP@$elb_ip@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@BAM_IP@$bam_ip@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@BAM_PORT@$bam_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@KEYPAIR_PATH@$keypair_path@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@SCRIPT_PATH@$script_path@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@HOST_USER@$host_user@g" > repository/conf/cartridge-config.properties

    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_HOSTNAME@$userstore_db_hostname@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_PORT@$userstore_db_port@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_USER@$userstore_db_user@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_PASS@$userstore_db_pass@g" > repository/conf/datasources/master-datasources.xml

    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_HOSTNAME@$billing_db_hostanme@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_PORT@$billing_db_port@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_SCHEMA@$billing_db_schema@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_USERNAME@$billing_db_username@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_PASSWORD@$billing_db_password@g" > repository/conf/datasources/stratos-datasources.xml
    
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@SC_HOSTNAME@${sc_hostname}@g" > repository/conf/axis2/axis2.xml
    
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@SC_CLUSTER_PORT@${sc_cluster_port}@g" > repository/conf/axis2/axis2.xml
    
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@SC_PORT_OFFSET@${sc_port_offset}@g" > repository/conf/carbon.xml
    
    cp -f repository/conf/log4j.properties repository/conf/log4j.properties.orig
    cat repository/conf/log4j.properties.orig | sed -e "s@BAM_HOSTNAME:BAM_RECEIVER_PORT@$bam_hostname:$bam_reciever_port@g" > repository/conf/log4j.properties

    cp -f repository/conf/etc/logging-config.xml repository/conf/etc/logging-config.xml.orig
    cat repository/conf/etc/logging-config.xml.orig | sed -e "s@BAM_HOSTNAME:CASSANDRA_PORT@$bam_hostname:$cassandra_port@g" > repository/conf/etc/logging-config.xml

    popd # sc_path


    # Database Configuration
    # -----------------------------------------------
    echo "Create and configure MySql Databases" >> $LOG

    echo "Creating userstore database"
    mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/userstore.sql
    #mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/registry.sql   #registry schema is only for AF
    
    echo "Creating stratos_foundation database"
    mysql -u$stratos_foundation_db_user -p$stratos_foundation_db_pass < $resource_path/stratos_foundation.sql

    #mysql -u$billing_db_username -p$billing_db_password < $resource_path/billing-mysql.sql

    #mysql -u$billing_db_username -p$billing_db_password < $resource_path/metering_mysql.sql

    #Namespace Binding
    # -----------------------------------------------
    echo "bind Namespaces" >> $LOG
    #apt-get install bind9 zip
    #Copy the /db.stratos.com file into /etc/bind. Edit it as necessary
    #cp -f ./resources/db.stratos.com $resource_path/db.$stratos_domain
    #echo "Set ELb Hostname in /etc/bind/db.stratos.com" >> $LOG
    #cat $resource_path/db.$stratos_domain | sed -e "s@SC_HOSTNAME@$sc_hostname@g" | sed -e "s@ELB_IP@$elb_ip@g" | sed -e "s@STRATOS_DOMAIN@$stratos_domain@g" > /etc/bind/db.$stratos_domain

    #echo "Add the following content to /etc/bind/named.conf.local" >> $LOG
    #echo "zone \"$stratos_domain\" {" >> /etc/bind/named.conf.local
    #echo "      type master;" >> /etc/bind/named.conf.local
    #echo "      file \"/etc/bind/db.$stratos_domain\";" >> /etc/bind/named.conf.local
    #echo "};" >> /etc/bind/named.conf.local

    #Copy https://svn.wso2.org/repos/wso2/scratch/hosting/build/tropos/resources/append_zone_file.sh into /opt/scripts folder
    if [[ ! -d $stratos_path/scripts ]]; then
        mkdir -p $stratos_path/scripts
    fi
    cp -f ./scripts/add_entry_zone_file.sh $stratos_path/scripts/add_entry_zone_file.sh
    cp -f ./scripts/remove_entry_zone_file.sh $stratos_path/scripts/remove_entry_zone_file.sh


    echo "End configuring the SC"
fi #End SC server installation


# ------------------------------------------------
# Setup CC
# ------------------------------------------------
if [[ $cc = "true" ]]; then
    echo "Setup CC" >> $LOG
    echo "Configuring the Cloud Controller"

    echo "Creating payload directory ... " >> $LOG
    if [[ ! -d $cc_path/repository/resources/payload ]]; then
        mkdir -p $cc_path/repository/resources/payload
    fi

    cp -f ./config/cc/repository/conf/cloud-controller.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/jndi.properties $cc_path/repository/conf/

    echo "In repository/conf/cloud-controller.xml"
    if [[ $ec2_provider_enabled = true ]]; then
        ./ec2.sh
    fi
    if [[ $openstack_provider_enabled = true ]]; then
        ./openstack.sh
    fi

    pushd $cc_path
    
    cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
    cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/cloud-controller.xml

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@CC_PORT_OFFSET@$cc_port_offset@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/jndi.properties

    popd #cc_path
    echo "End configuring the Cloud Controller"
fi


# ------------------------------------------------
# Setup LB
# ------------------------------------------------    
if [[ $lb = "true" ]]; then
    echo "Setup LB" >> $LOG
    echo "Configuring the Load Balancer"

    cp -f ./config/lb/repository/conf/loadbalancer.conf $lb_path/repository/conf/
    cp -f ./config/lb/repository/conf/axis2/axis2.xml $lb_path/repository/conf/axis2/

    pushd $lb_path

    echo "In repository/conf/loadbalancer.conf" >> $LOG
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@MB_IP@$mb_ip@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@MB_LISTEN_PORT@$mb_listen_port@g" > repository/conf/loadbalancer.conf
    
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@CEP_IP@$cep_ip@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@CEP_LISTEN_PORT@$cep_listen_port@g" > repository/conf/loadbalancer.conf

    popd #lb_path
    echo "End configuring the Load Balancer"
fi


# ------------------------------------------------
# Setup AS
# ------------------------------------------------   
if [[ $as = "true" ]]; then
    echo "Setup AS" >> $LOG
    echo "Configuring the Auto Scalar"

    cp -f ./config/as/repository/conf/carbon.xml $as_path/repository/conf/
    cp -f ./config/as/repository/conf/jndi.properties $as_path/repository/conf/

    pushd $as_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@AS_PORT_OFFSET@$as_port_offset@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/jndi.properties

    popd #as_path
    echo "End configuring the Auto Scalar"
fi


# Setup BAM
# --------------------------------------------------
if [[ $bam = "true" ]]; then
 echo "Setup BAM" >> $LOG
 echo "Configuring the BAM"
 cp -f ./config/bam/bin/stratos.sh $bam_path/bin/
 cp -f ./config/bam/repository/conf/carbon.xml $bam_path/repository/conf/
 cp -f ./config/bam/repository/conf/etc/cassandra-component.xml $bam_path/repository/conf/etc/
 cp -f ./config/bam/repository/conf/etc/cassandra.yaml $bam_path/repository/conf/etc/
 cp -f ./config/bam/repository/conf/datasources/master-datasources.xml $bam_path/repository/conf/datasources/
 cp -f $mysql_connector_jar $bam_path/repository/components/lib/

  pushd $bam_path

  cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
  cat repository/conf/carbon.xml.orig | sed -e "s@BAM_PORT_OFFSET@${bam_port_offset}@g" > repository/conf/carbon.xml

  cp -f repository/conf/etc/cassandra.yaml repository/conf/etc/cassandra.yaml.orig
  cat repository/conf/etc/cassandra.yaml.orig | sed -e "s@BAM_HOSTNAME@${bam_hostname}@g" > repository/conf/etc/cassandra.yaml

  cp -f repository/conf/etc/cassandra-component.xml repository/conf/etc/cassandra-component.xml.orig
  cat repository/conf/etc/cassandra-component.xml.orig | sed -e "s@BAM_HOSTNAME:CASSANDRA_PORT@$bam_hostname:$cassandra_port@g" > repository/conf/etc/cassandra-component.xml

 echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_HOSTNAME@$billing_db_hostanme@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_PORT@$billing_db_port@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_SCHEMA@$billing_db_schema@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_USERNAME@$billing_db_username@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_PASSWORD@$billing_db_password@g" > repository/conf/datasources/master-datasources.xml

 popd #bam_path
fi


# Configure cartridges
# ---------------------------------------------------------
if [[ "$openstack_provider_enabled" = "true" ]]; then
    ./openstack-cartridge.sh
fi
if [[ "$ec2_provider_enabled" = "true" ]]; then
    ./ec2-cartridge.sh
fi

echo 'Changing owner of '$stratos_path' to '$host_user:$host_user
chown $host_user:$host_user $stratos_path -R

echo "Apache Stratos setup has successfully completed"

read -p "Do you want to start the servers [y/n]? " answer
if [[ $answer != y ]] ; then
    exit 1
fi


# Starting the servers
# ---------------------------------------------------------
echo "Starting the servers" >> $LOG
#Starting the servers in the following order is recommended
#mb, cc, elb, is, agent, sc

echo "Starting up servers. This may take time. Look at $LOG file for server startup details"

chown -R $host_user.$host_user $log_path
chmod -R 777 $log_path

export setup_dir=$PWD
su - $host_user -c "source $setup_dir/conf/setup.conf;$setup_dir/start-servers.sh -p$product_list >> $LOG"

echo "Servers started. Please look at $LOG file for server startup details"
if [[ $sc == "true" ]]; then
    echo "**************************************************************"
    echo "Management Console : https://$stratos_domain:$sc_https_port/"
    echo "**************************************************************"
fi

