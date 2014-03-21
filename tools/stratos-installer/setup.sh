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
as="false"
sm="false"
cep="false"
product_list="mb;cc;cep;as;sm"
enable_internal_git=false

function help {
    echo ""
    echo "Usage:"
    echo "setup.sh -u <host username> -p \"<product list>\""
    echo "product list : [mb, cc, as, sm, cep]"
    echo "Example:"
    echo "sudo ./setup.sh -p \"cc\""
    echo "sudo ./setup.sh -p \"all\""
    echo ""
    echo "-u: <host username> The login user of the host."
    echo "-p: <product list> Apache Stratos products to be installed on this node. Provide one or more names of the servers."
    echo "    The available servers are cc, as, sm or all. 'all' means you need to setup all servers in this machine. Default is all"
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
    if [[ $x = "as" ]]; then
        as="true"
    fi
    if [[ $x = "sm" ]]; then
        sm="true"
    fi
    if [[ $x = "all" ]]; then
        mb="true"
        cep="true"
        cc="true"
        as="true"
        sm="true"
    fi
done

product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi

if [[ $host_user == "" ]]; then
    echo "user provided in conf/setup.conf is null. Please provide a user"
    exit 1
fi

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

export $host_user

# Check validity of IP
function valid_ip()
{
    local  ip=$1
    local  stat=1

    if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
        OIFS=$IFS
        IFS='.'
        ip=($ip)
        IFS=$OIFS
        [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
            && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
        stat=$?
    fi
    return $stat
}

function helpsetup {
    echo ""
    echo "Please set up the $1 related environment variables correctly in conf/setup.conf"
    echo ""
}

function general_conf_validate {
    if [[ ! -d $setup_path ]]; then
        echo "Please specify the setup_path folder which contains stratos setup"
        exit 1
    fi
    if [[ ! -d $stratos_pack_path ]]; then
        echo "Please specify the stratos_pack_path folder which contains stratos packages"
        exit 1
    fi
    if [[ ! -d $stratos_path ]]; then
        echo "Please specify the stratos_path folder which stratos will be installed"
        exit 1
    fi
    if [[ ! -d $JAVA_HOME ]]; then
        echo "Please set the JAVA_HOME environment variable for the running user"
        exit 1
    fi
    export JAVA_HOME=$JAVA_HOME

    if [[ -z $stratos_domain ]]; then
        echo "Please specify the stratos domain"
        exit 1
    fi
    if [[ (-z $mb_port_offset || -z $mb_ip || -z $mb_hostname) ]]; then
        echo "Please specify the ip, the port offset and the hostname of MB"
        exit 1
    fi
    if !(valid_ip $mb_ip); then 
        echo "Please provide valid ip for MB's ip"
        exit 1
    fi
}

function andes_jar_validate {
    if [[ ($cep = "true" || $cc = "true" || $as = "true" || $sm = "true") ]]; then
        if [[ ! -f $andes_client_jar ]]; then
            echo "Please copy the andes client jar into the same folder as this command(stratos release pack folder) and update conf/setup.conf file"
            exit 1
        fi
    fi
}

function mb_conf_validate {
    if [[ -z $mb_path ]]; then
	helpsetup MB
	exit 1
    fi
}

function cep_conf_validate {
    if [[ (-z $cep_path || -z $cep_port_offset) ]]; then
	helpsetup CEP
	exit 1
    fi
    if [[ ! -d $cep_artifacts_path ]]; then
        echo "Please specify the cep_artifacts_path folder which contains cep artifacts files"
        exit 1
    fi
    if [[ ! -f $cep_extension_jar ]]; then
        echo "Please copy the cep extension jar into the same folder as this command(stratos release pack folder) and update conf/setup.conf file"
        exit 1
    fi
}

function cc_conf_validate {
    if [[ (-z $cc_path || -z $cc_port_offset) ]]; then
	helpsetup CC
	exit 1
    fi

    if [[ $ec2_provider_enabled = "false" && $openstack_provider_enabled = "false" && $vcloud_provider_enabled = "false" ]]; then
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

    if [[ $vcloud_provider_enabled = "true" ]]; then
        if [[ ( -z $vcloud_identity || -z $vcloud_credential || -z $vcloud_jclouds_endpoint ) ]]; then
            echo "Please set vcloud configuration information in conf/setup.conf file"
            exit 1
        fi
    fi
}

function as_conf_validate {
    if [[ (-z $as_path || -z $as_port_offset) ]]; then
	helpsetup AS
	exit 1
    fi
    if [[ -z $cc_port_offset || -z $sm_port_offset ]]; then
        echo "Please specify the port offset of SM and/or CC"
        exit 1
    fi
    if [[ -z $sm_ip || -z $cc_ip ]]; then
        echo "Please specify the ips of SM and/or CC"
        exit 1
    elif !(valid_ip $sm_ip && valid_ip $cc_ip ); then
        echo "Please provide valid ips for SM and/or CC"
        exit 1
    fi
    if [[ -z $cc_hostname || -z $sm_hostname ]]; then
	echo "Please specify valid hostname for SM and/or CC"
	exit 1
    fi
}

function sm_conf_validate {
    if [[ (-z $sm_path || -z $sm_port_offset) ]]; then
	helpsetup SM
	exit 1
    fi
    if [[ ! -f $mysql_connector_jar ]]; then
        echo "Please copy the mysql connector jar to the stratos release pack folder and update the JAR name in conf/setup.conf file"
        exit 1
    fi
    if [[ -z $cc_port_offset || -z $as_port_offset ]]; then
        echo "Please specify the port offset of AS and/or CC"
        exit 1
    fi
    if [[ -z $sm_ip || -z $as_ip || -z $cc_ip ]]; then
        echo "Please specify the ips of SM and/or AS and/or CC"
        exit 1
    elif !(valid_ip $sm_ip && valid_ip $cc_ip && valid_ip $as_ip); then
        echo "Please provide valid ips for SM and/or AS and/or CC"
        exit 1
    fi
    if [[ -z $puppet_ip ]]; then
        echo "Please specify the ip of puppet master"
        exit 1
    elif !(valid_ip $puppet_ip); then
        echo "Please provide valid ip for puppet master"
        exit 1
    fi
    if [[ -z $puppet_hostname ]]; then
        echo "Please specify the puppet master's hostname"
        exit 1
    fi
    if [[ -z $puppet_environment ]]; then
        echo "Please specify the relevant puppet environment"
        exit 1
    fi
    if [[ -z $cc_hostname || -z $as_hostname ]]; then
	echo "Please specify valid hostname for AS and/or CC"
	exit 1
    fi
}


general_conf_validate
andes_jar_validate
if [[ $mb = "true" ]]; then
    mb_conf_validate
fi
if [[ $cep = "true" ]]; then
    cep_conf_validate
fi
if [[ $cc = "true" ]]; then
    cc_conf_validate
fi
if [[ $as = "true" ]]; then
    as_conf_validate
fi
if [[ $sm = "true" ]]; then
    sm_conf_validate
fi


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
        echo "Extracting Message Broker"
        unzip -q $mb_pack_path -d $stratos_path
    fi
fi
if [[ $cep = "true" ]]; then
    if [[ ! -d $cep_path ]]; then
        echo "Extracting Complex Event Processor"
        unzip -q $cep_pack_path -d $stratos_path
    fi
fi
if [[ $cc = "true" ]]; then
    if [[ ! -d $cc_path ]]; then
        echo "Extracting Cloud Controller"
        unzip -q $cc_pack_path -d $stratos_path
    fi
fi
if [[ $as = "true" ]]; then
    if [[ ! -d $as_path ]]; then
        echo "Extracting Autoscaler"
        unzip -q $as_pack_path -d $stratos_path
    fi
fi
if [[ $sm = "true" ]]; then
    if [[ ! -d $resource_path ]]; then
        cp -rf ./resources $stratos_path
    fi
    if [[ ! -d $sm_path ]]; then
        echo "Extracting Stratos Manager"
        unzip -q $sm_pack_path -d $stratos_path
    fi
fi



# ------------------------------------------------
# Setup MB
# ------------------------------------------------
function mb_setup {
    echo "Setup MB" >> $LOG
    echo "Configuring the Message Broker"

    pushd $mb_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@<Offset>0</Offset>@<Offset>${mb_port_offset}</Offset>@g" > repository/conf/carbon.xml

    echo "End configuring the Message Broker"
    popd #mb_path
}

if [[ $mb = "true" ]]; then
    mb_setup
fi

# ------------------------------------------------
# Setup CEP
# ------------------------------------------------
function cep_setup {
    echo "Setup CEP" >> $LOG
    echo "Configuring the Complex Event Processor"

    cp -f ./config/cep/repository/conf/jndi.properties $cep_path/repository/conf/
    cp -f $cep_extension_jar $cep_path/repository/components/lib/
    cp -f $andes_client_jar $cep_path/repository/components/dropins/
    cp -f $cep_artifacts_path/eventbuilders/*.xml $cep_path/repository/deployment/server/eventbuilders/
    cp -f $cep_artifacts_path/inputeventadaptors/*.xml $cep_path/repository/deployment/server/inputeventadaptors/
    cp -f $cep_artifacts_path/outputeventadaptors/*.xml $cep_path/repository/deployment/server/outputeventadaptors/
    cp -f $cep_artifacts_path/executionplans/*.xml $cep_path/repository/deployment/server/executionplans/
    cp -f $cep_artifacts_path/eventformatters/*.xml $cep_path/repository/deployment/server/eventformatters/
    cp -f $cep_artifacts_path/streamdefinitions/*.xml $cep_path/repository/conf/

    pushd $cep_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@<Offset>0</Offset>@<Offset>${cep_port_offset}</Offset>@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$cep_mb_listen_port@g" > repository/conf/jndi.properties

    echo "In outputeventadaptors"
    cp -f repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml.orig
    cat repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml.orig | sed -e "s@CEP_HOME@$cep_path@g" > repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml

    echo "In repository/conf/siddhi/siddhi.extension"
    cp -f repository/conf/siddhi/siddhi.extension repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.GradientFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.SecondDerivativeFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.FaultHandlingWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    mv -f repository/conf/siddhi/siddhi.extension.orig repository/conf/siddhi/siddhi.extension

    echo "End configuring the Complex Event Processor"
    popd #cep_path
}
if [[ $cep = "true" ]]; then
    cep_setup
fi


# ------------------------------------------------
# Setup CC
# ------------------------------------------------
function cc_setup {
    echo "Setup CC" >> $LOG
    echo "Configuring the Cloud Controller"

    echo "Creating payload directory ... " >> $LOG
    if [[ ! -d $cc_path/repository/resources/payload ]]; then
        mkdir -p $cc_path/repository/resources/payload
    fi

    cp -f ./config/cc/repository/conf/cloud-controller.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/jndi.properties $cc_path/repository/conf/
    cp -f $andes_client_jar $cc_path/repository/components/dropins/

    echo "In repository/conf/cloud-controller.xml"
    if [[ $ec2_provider_enabled = true ]]; then
        ./ec2.sh
    fi
    if [[ $openstack_provider_enabled = true ]]; then
        ./openstack.sh
    fi
    if [[ $vcloud_provider_enabled = true ]]; then
        ./vcloud.sh
    fi

    pushd $cc_path
    
    cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
    cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$cc_mb_listen_port@g" > repository/conf/cloud-controller.xml

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@CC_PORT_OFFSET@$cc_port_offset@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$cc_mb_listen_port@g" > repository/conf/jndi.properties

    popd #cc_path
    echo "End configuring the Cloud Controller"
}

if [[ $cc = "true" ]]; then
   cc_setup
fi

# ------------------------------------------------
# Setup AS
# ------------------------------------------------   
function as_setup {
    echo "Setup AS" >> $LOG
    echo "Configuring the Auto Scalar"

    cp -f ./config/as/repository/conf/carbon.xml $as_path/repository/conf/
    cp -f ./config/as/repository/conf/jndi.properties $as_path/repository/conf/
    cp -f ./config/as/repository/conf/autoscaler.xml $as_path/repository/conf/
    cp -f $andes_client_jar $as_path/repository/components/dropins/

    pushd $as_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@AS_PORT_OFFSET@$as_port_offset@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$as_mb_listen_port@g" > repository/conf/jndi.properties

    echo "In repository/conf/autoscaler.xml"
    cp -f repository/conf/autoscaler.xml repository/conf/autoscaler.xml.orig
    cat repository/conf/autoscaler.xml.orig | sed -e "s@CC_HOSTNAME@$cc_hostname@g" > repository/conf/autoscaler.xml

    cp -f repository/conf/autoscaler.xml repository/conf/autoscaler.xml.orig
    cat repository/conf/autoscaler.xml.orig | sed -e "s@CC_LISTEN_PORT@$as_cc_https_port@g" > repository/conf/autoscaler.xml

    cp -f repository/conf/autoscaler.xml repository/conf/autoscaler.xml.orig
    cat repository/conf/autoscaler.xml.orig | sed -e "s@SM_HOSTNAME@$sm_hostname@g" > repository/conf/autoscaler.xml

    cp -f repository/conf/autoscaler.xml repository/conf/autoscaler.xml.orig
    cat repository/conf/autoscaler.xml.orig | sed -e "s@SM_LISTEN_PORT@$as_sm_https_port@g" > repository/conf/autoscaler.xml

    popd #as_path
    echo "End configuring the Auto smalar"
}

if [[ $as = "true" ]]; then
    as_setup
fi



# ------------------------------------------------
# Setup SM
# ------------------------------------------------
function sm_setup {
    echo "Setup SM" >> $LOG
    echo "Configuring Stratos Manager"

    cp -f ./config/sm/repository/conf/carbon.xml $sm_path/repository/conf/
    cp -f ./config/sm/repository/conf/jndi.properties $sm_path/repository/conf/
    cp -f ./config/sm/repository/conf/cartridge-config.properties $sm_path/repository/conf/
    cp -f ./config/sm/repository/conf/datasources/master-datasources.xml $sm_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $sm_path/repository/components/lib/
    cp -f $andes_client_jar $sm_path/repository/components/dropins/

    pushd $sm_path

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@SC_PORT_OFFSET@$sm_port_offset@g" > repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    cp -f repository/conf/jndi.properties repository/conf/jndi.properties.orig
    cat repository/conf/jndi.properties.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$sm_mb_listen_port@g" > repository/conf/jndi.properties

    echo "In repository/conf/cartridge-config.properties" >> $LOG
    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@CC_HOSTNAME:CC_HTTPS_PORT@$cc_hostname:$sm_cc_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@AS_HOSTNAME:AS_HTTPS_PORT@$as_hostname:$sm_as_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@PUPPET_IP@$sm_puppet_ip@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@PUPPET_HOSTNAME@$sm_puppet_hostname@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@PUPPET_ENV@$sm_puppet_environment@g" > repository/conf/cartridge-config.properties

    echo "In repository/conf/datasources/master-datasources.xml" >> $LOG
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

    popd # sm_path


    # Database Configuration
    # -----------------------------------------------
    echo "Create and configure MySql Databases" >> $LOG 

    echo "Creating userstore database"

    pushd $resource_path
    cp -f mysql.sql mysql.sql.orig
    cat mysql.sql.orig | sed -e "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g" > mysql.sql

    popd # resource_path

    mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/mysql.sql
    
    #Copy https://svn.wso2.org/repos/wso2/scratch/hosting/build/tropos/resources/append_zone_file.sh into /opt/scripts folder
    if [[ ! -d $stratos_path/scripts ]]; then
        mkdir -p $stratos_path/scripts
    fi
    cp -f ./scripts/add_entry_zone_file.sh $stratos_path/scripts/add_entry_zone_file.sh
    cp -f ./scripts/remove_entry_zone_file.sh $stratos_path/scripts/remove_entry_zone_file.sh


    echo "End configuring the SM"
}

if [[ $sm = "true" ]]; then
    sm_setup
fi

 
# ------------------------------------------------
# Mapping domain/host names 
# ------------------------------------------------

cp -f /etc/hosts hosts.tmp


echo "$mb_ip $mb_hostname	# message broker hostname"	>> hosts.tmp

if [[ $sm = "true" || $as = "true" ]]; then
    echo "$sm_ip $sm_hostname	# stratos domain"	>> hosts.tmp
    echo "$cc_ip $cc_hostname	# cloud controller hostname"	>> hosts.tmp
fi

if [[ $sm = "true" ]]; then
    echo "$as_ip $as_hostname	# auto scalar hostname"	>> hosts.tmp
fi

mv -f ./hosts.tmp /etc/hosts


# ------------------------------------------------
# Starting the servers
# ------------------------------------------------
echo 'Changing owner of '$stratos_path' to '$host_user:$host_user
chown $host_user:$host_user $stratos_path -R

echo "Apache Stratos setup has successfully completed"

read -p "Do you want to start the servers [y/n]? " answer
if [[ $answer != y ]] ; then
   exit 1
fi

echo "Starting the servers" >> $LOG

echo "Starting up servers. This may take time. Look at $LOG file for server startup details"

chown -R $host_user.$host_user $log_path
chmod -R 777 $log_path

export setup_dir=$PWD
su - $host_user -c "source $setup_dir/conf/setup.conf;$setup_dir/start-servers.sh -p\"$product_list\" >> $LOG"

echo "Servers started. Please look at $LOG file for server startup details"
if [[ $sm == "true" ]]; then
    echo "**************************************************************"
    echo "Management Console : https://$stratos_domain:$sm_https_port/console"
    echo "**************************************************************"
fi



