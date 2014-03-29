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

source "./conf/stratos-setup.conf"
export LOG=$log_path/stratos-setup.log

profile_list="default;cc;as;sm"
profile="default"
config_mb="true"
activemq_client_libs=(activemq-broker-5.8.0.jar  activemq-client-5.8.0.jar  geronimo-j2ee-management_1.1_spec-1.0.1.jar  geronimo-jms_1.1_spec-1.1.1.jar  hawtbuf-1.2.jar)
auto_start_servers="false"

function help {
    echo ""
    echo "Usage:"
    echo "setup.sh -p \"<profile>\" [-s]"
    echo "product list : [default, cc, as, sm]"
    echo "Example:"
    echo "sudo ./setup.sh -p \"default\""
    echo "sudo ./setup.sh -p \"cc\""
    echo ""
    echo "-p: <profile> Apache Stratos products to be installed on this node. Provide one name of a profile."
    echo "    The available profiles are cc, as, sm or default. 'default' means you need to setup all servers in this machine. Default is 'default' profile"
    echo "-s: Start servers after installation."
    echo ""
}

while getopts p: opts
do
  case $opts in
    p)
        profile_list=${OPTARG}
        ;;
    s)
        auto_start_servers="true"
        ;;
    \?)
        help
        exit 1
        ;;
  esac
done


arr=$(echo $profile_list | tr " " "\n")

for x in $arr
do
    if [[ $x = "cc" ]]; then
        profile="cc"
    elif [[ $x = "as" ]]; then
        profile="as"
    elif [[ $x = "sm" ]]; then
        profile="sm"
    else
        echo "Inavlid profile : 'default' profile will be selected."
    fi
done

echo "You have selected profile : $profile"

profile_list=`echo $profile_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $profile_list || $profile_list = "" ]]; then
    help
    exit 1
fi

if [[ $host_user == "" ]]; then
    echo "user provided in conf/stratos-setup.conf is null. Please provide a user"
    exit 1
fi

echo "user provided in conf/stratos-setup.conf is $host_user. If you want to provide some other user name please specify it at the prompt."
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

function helpsetup() {
    echo ""
    echo "Please set up the $1 related environment variables correctly in conf/stratos-setup.conf"
    echo ""
}

function general_conf_validate() {
    if [[ ! -d $setup_path ]]; then
        echo "Please specify the setup_path folder which contains stratos setup"
        exit 1
    fi
    if [[ ! -d $stratos_packs ]]; then
        echo "Please specify the stratos_packs folder which contains stratos packages"
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
    if [[ ! -f $stratos_pack_zip ]]; then
        echo "Please copy the startos zip to the stratos pack folder"
        exit 1
    fi
    if [[ -z $mb_port ]]; then
        echo "Please specify the port of MB"
        exit 1
    fi

    if [[ $profile = "default" ]]; then
        read -p "Do you want to configure ActiveMQ [y/n]: " answer
        if [[ $answer = y ]] ; then
            mb_ip=$host_ip
        else
            echo "Provided mb_ip in conf/stratos-setup.conf will be used"
            config_mb="false"
        fi
    fi
}

function cc_related_popup() {
    while read -p "Please provide cloud controller ip:" cc_ip
    do
	if !(valid_ip $cc_ip); then
	    echo "Please provide valid ips for CC"	 
	else 
            export cc_ip
	    break 
	fi
    done 

    while read -p "Please provide cloud controller hostname:" cc_hostname
    do
	if [[ -z $cc_hostname ]]; then
	    echo "Please specify valid hostname for CC"	 
	else 
            export cc_hostname
	    break 
	fi
    done

    while read -p "Please provide cloud controller port offset:" cc_port_offset
    do
	if [[ -z $cc_port_offset ]]; then
	    echo "Please specify the port offset of CC"	 
	else 
            export cc_port_offset
	    break 
	fi
    done
}

function sm_related_popup() {
    while read -p "Please provide Stratos Manager ip:" sm_ip
    do
	if !(valid_ip $sm_ip); then
	    echo "Please provide valid ips for SM"	 
	else 
            export sm_ip
	    break 
	fi
    done 

    while read -p "Please provide Stratos Manager hostname:" sm_hostname
    do
	if [[ -z $sm_hostname ]]; then
	    echo "Please specify valid hostname for SM"	 
	else 
            export sm_hostname
	    break 
	fi
    done

    while read -p "Please provide Stratos Manager port offset:" sm_port_offset
    do
	if [[ -z $sm_port_offset ]]; then
	    echo "Please specify the port offset of SM"	 
	else 
            export sm_port_offset
	    break 
	fi
    done
}

function as_related_popup() {
    while read -p "Please provide Auto Scalar ip:" as_ip
    do
	if !(valid_ip $as_ip); then
	    echo "Please provide valid ips for AS"	 
	else 
            export as_ip
	    break 
	fi
    done 

    while read -p "Please provide Auto Scala hostname:" as_hostname
    do
	if [[ -z $as_hostname ]]; then
	    echo "Please specify valid hostname for AS"	 
	else 
            export as_hostname
	    break 
	fi
    done

    while read -p "Please provide Auto Scala port offset:" as_port_offset
    do
	if [[ -z $as_port_offset ]]; then
	    echo "Please specify the port offset of AS"	 
	else 
            export as_port_offset
	    break 
	fi
    done
}

function mb_jars_validate() {
    for activemq_client_lib in "${activemq_client_libs[@]}"
    do
	lib_path=$stratos_packs/$activemq_client_lib
	if [[ ! -f $lib_path ]]; then
	    echo "Please copy the $activemq_client_lib into the stratos pack folder"
	    exit 1
	fi
    done
}

function activemq_validate() {
    if [[ ! -f $activemq_pack ]]; then
        echo "Please copy the activemq zip to the stratos pack folder and update the JAR name in conf/stratos-setup.conf file"
        exit 1
    fi
}

function cc_conf_validate() {
    if [[ $ec2_provider_enabled = "false" && $openstack_provider_enabled = "false" && $vcloud_provider_enabled = "false" ]]; then
        echo "Please enable at least one of the IaaS providers in conf/stratos-setup.conf file"
        exit 1
    fi
    if [[ $openstack_provider_enabled = "true" ]]; then
        if [[ ( -z $openstack_identity || -z $openstack_credential || -z $openstack_jclouds_endpoint ) ]]; then
            echo "Please set openstack configuration information in conf/stratos-setup.conf file"
            exit 1
        fi
    fi
    if [[ $ec2_provider_enabled = "true" ]]; then
        if [[ ( -z $ec2_identity || -z $ec2_credential || -z $ec2_keypair_name ) ]]; then
            echo "Please set ec2 configuration information in conf/stratos-setup.conf file"
            exit 1
        fi
    fi
    if [[ $vcloud_provider_enabled = "true" ]]; then
        if [[ ( -z $vcloud_identity || -z $vcloud_credential || -z $vcloud_jclouds_endpoint ) ]]; then
            echo "Please set vcloud configuration information in conf/stratos-setup.conf file"
            exit 1
        fi
    fi
}


function as_conf_validate() {
    if [[ !($profile = "default") ]]; then
	cc_related_popup
	sm_related_popup
	export as_cc_https_port=$((9443 + $cc_port_offset))
	export as_sm_https_port=$((9443 + $sm_port_offset))
    else
        cc_hostname=$stratos_domain
        sm_hostname=$stratos_domain
	export as_cc_https_port=$((9443 + $offset))
	export as_sm_https_port=$((9443 + $offset))
    fi
}


function sm_conf_validate() {
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
    if [[ ! -f $mysql_connector_jar ]]; then
        echo "Please copy the mysql connector jar to the stratos release pack folder and update the JAR name in conf/stratos-setup.conf file"
        exit 1
    fi

    if [[ !($profile = "default") ]]; then
	cc_related_popup
	as_related_popup
	export sm_cc_https_port=$((9443 + $cc_port_offset))
	export sm_as_https_port=$((9443 + $as_port_offset))
    else
        export cc_hostname=$stratos_domain
        export as_hostname=$stratos_domain
	export sm_cc_https_port=$((9443 + $offset))
	export sm_as_https_port=$((9443 + $offset))
    fi
    export sm_https_port=$((9443 + $offset))
}


function cep_conf_validate() {
    if [[ ! -d $cep_artifacts_path ]]; then
        echo "Please specify the cep_artifacts_path folder which contains cep artifacts files"
        exit 1
    fi
    if [[ ! -f $cep_extension_jar ]]; then
        echo "Please copy the cep extension jar into the same folder as this command(stratos release pack folder) and update conf/stratos-setup.conf file"
        exit 1
    fi
}


# Make sure the user is running as root.
if [ "$UID" -ne "0" ]; then
	echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
	exit 69
fi

general_conf_validate
mb_jars_validate
if [[ $profile = "cc" ]]; then
    cc_conf_validate
elif [[ $profile = "as" ]]; then
    as_conf_validate
elif [[ $profile = "sm" ]]; then
    sm_conf_validate
else
    echo "In default profile CEP will be configured."
    activemq_validate
    cc_conf_validate
    as_conf_validate
    sm_conf_validate 
    cep_conf_validate  
fi

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi


echo ""
echo "For all the questions asked while during executing the script please just press the enter button"
echo ""


# Extract stratos zip file
if [[ !(-d $stratos_extract_path) ]]; then
    echo "Extracting Apache Stratos"
    unzip -q $stratos_pack_zip -d $stratos_path
    mv -f $stratos_path/apache-stratos-4.0.0-SNAPSHOT $stratos_extract_path
fi

if [[ ($profile = "default" && $config_mb = "true") ]]; then
    echo "Extracting ActiveMQ"
    tar -xzf $activemq_pack -C $stratos_path
fi

# ------------------------------------------------
# Setup General
# ------------------------------------------------
function general_setup() {

    cp -f ./config/all/repository/conf/activemq/jndi.properties $stratos_extract_path/repository/conf/

    pushd $stratos_extract_path
    echo "In repository/conf/carbon.xml"
    sed -i "s@<Offset>0</Offset>@<Offset>${offset}</Offset>@g" repository/conf/carbon.xml

    echo "In repository/conf/jndi.properties"
    sed -i "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_ip:$mb_port@g" repository/conf/jndi.properties
    popd

    for activemq_client_lib in "${activemq_client_libs[@]}" 
    do
    	cp -f $stratos_packs/$activemq_client_lib $stratos_extract_path/repository/components/lib/
    done
}


# ------------------------------------------------
# Setup cc
# ------------------------------------------------
function cc_setup() {
    echo "Setup CC" >> $LOG
    echo "Configuring the Cloud Controller"

    cp -f ./config/all/repository/conf/cloud-controller.xml $stratos_extract_path/repository/conf/ 

    export cc_path=$stratos_extract_path
    echo "In repository/conf/cloud-controller.xml"
    if [[ $ec2_provider_enabled = true ]]; then
        ./stratos-ec2.sh
    fi
    if [[ $openstack_provider_enabled = true ]]; then
        ./stratos-openstack.sh
    fi
    if [[ $vcloud_provider_enabled = true ]]; then
        ./stratos-vcloud.sh
    fi

    pushd $stratos_extract_path
    
    popd 
    echo "End configuring the Cloud Controller"
}


# ------------------------------------------------
# Setup AS
# ------------------------------------------------   
function as_setup() {
    echo "Setup AS" >> $LOG
    echo "Configuring the Auto Scalar"

    cp -f ./config/all/repository/conf/autoscaler.xml $stratos_extract_path/repository/conf/

    pushd $stratos_extract_path

    echo "In repository/conf/autoscaler.xml"
    sed -i "s@CC_HOSTNAME@$cc_hostname@g" repository/conf/autoscaler.xml
    sed -i "s@CC_LISTEN_PORT@$as_cc_https_port@g" repository/conf/autoscaler.xml
    sed -i "s@SM_HOSTNAME@$sm_hostname@g" repository/conf/autoscaler.xml
    sed -i "s@SM_LISTEN_PORT@$as_sm_https_port@g" repository/conf/autoscaler.xml

    popd
    echo "End configuring the Auto scalar"
}


# ------------------------------------------------
# Setup SM
# ------------------------------------------------
function sm_setup() {
    echo "Setup SM" >> $LOG
    echo "Configuring Stratos Manager"

    cp -f ./config/all/repository/conf/cartridge-config.properties $stratos_extract_path/repository/conf/
    cp -f ./config/all/repository/conf/datasources/master-datasources.xml $stratos_extract_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $stratos_extract_path/repository/components/lib/

    pushd $stratos_extract_path

    echo "In repository/conf/cartridge-config.properties"
    sed -i "s@CC_HOSTNAME:CC_HTTPS_PORT@$cc_hostname:$sm_cc_https_port@g" repository/conf/cartridge-config.properties
    sed -i "s@AS_HOSTNAME:AS_HTTPS_PORT@$as_hostname:$sm_as_https_port@g" repository/conf/cartridge-config.properties
    sed -i "s@PUPPET_IP@$puppet_ip@g" repository/conf/cartridge-config.properties
    sed -i "s@PUPPET_HOSTNAME@$puppet_hostname@g" repository/conf/cartridge-config.properties
    sed -i "s@PUPPET_ENV@$puppet_environment@g" repository/conf/cartridge-config.properties

    echo "In repository/conf/datasources/master-datasources.xml"
    sed -i "s@USERSTORE_DB_HOSTNAME@$userstore_db_hostname@g" repository/conf/datasources/master-datasources.xml
    sed -i "s@USERSTORE_DB_PORT@$userstore_db_port@g" repository/conf/datasources/master-datasources.xml
    sed -i "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g" repository/conf/datasources/master-datasources.xml
    sed -i "s@USERSTORE_DB_USER@$userstore_db_user@g" repository/conf/datasources/master-datasources.xml
    sed -i "s@USERSTORE_DB_PASS@$userstore_db_pass@g" repository/conf/datasources/master-datasources.xml

    popd

    # Database Configuration
    # -----------------------------------------------
    echo "Create and configure MySql Databases" >> $LOG 
    echo "Creating userstore database"

    pushd $resource_path
    sed -i "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g" mysql.sql

    popd

    mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/mysql.sql
    echo "End configuring the SM"
}


# ------------------------------------------------
# Setup CEP
# ------------------------------------------------
function cep_setup() {
    echo "Setup CEP" >> $LOG
    echo "Configuring the Complex Event Processor"

    cp -f $cep_extension_jar $stratos_extract_path/repository/components/lib/
    cep_xml_path=$stratos_extract_path/repository/deployment/server

    test -d "$cep_xml_path/eventbuilders" || mkdir -p "$cep_xml_path/eventbuilders" && cp -f $cep_artifacts_path/eventbuilders/*.xml $cep_xml_path/eventbuilders/
    test -d "$cep_xml_path/inputeventadaptors" || mkdir -p "$cep_xml_path/inputeventadaptors" && cp -f $cep_artifacts_path/inputeventadaptors/*.xml $cep_xml_path/inputeventadaptors/
    test -d "$cep_xml_path/outputeventadaptors" || mkdir -p "$cep_xml_path/outputeventadaptors" && cp -f $cep_artifacts_path/outputeventadaptors/*.xml $cep_xml_path/outputeventadaptors/
    test -d "$cep_xml_path/executionplans" || mkdir -p "$cep_xml_path/executionplans" && cp -f $cep_artifacts_path/executionplans/*.xml $cep_xml_path/executionplans/
    test -d "$cep_xml_path/eventformatters" || mkdir -p "$cep_xml_path/eventformatters" && cp -f $cep_artifacts_path/eventformatters/*.xml $cep_xml_path/eventformatters/
    test -d "$cep_xml_path/streamdefinitions" || mkdir -p "$cep_xml_path/streamdefinitions" && cp -f $cep_artifacts_path/streamdefinitions/*.xml $cep_xml_path/streamdefinitions/

    pushd $stratos_extract_path

    echo "In outputeventadaptors"
    sed -i "s@CEP_HOME@$stratos_extract_path@g" repository/deployment/server/outputeventadaptors/JMSOutputAdaptor.xml

    echo "In repository/conf/siddhi/siddhi.extension"
    test -d "repository/conf/siddhi" || mkdir -p "repository/conf/siddhi" && touch repository/conf/siddhi/siddhi.extension
    cp -f repository/conf/siddhi/siddhi.extension repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.GradientFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.SecondDerivativeFinderWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    echo "org.apache.stratos.cep.extension.FaultHandlingWindowProcessor" >> repository/conf/siddhi/siddhi.extension.orig
    mv -f repository/conf/siddhi/siddhi.extension.orig repository/conf/siddhi/siddhi.extension

    echo "End configuring the Complex Event Processor"
    popd
}


general_setup
if [[ $profile = "cc" ]]; then
    cc_setup
elif [[ $profile = "as" ]]; then
    as_setup
elif [[ $profile = "sm" ]]; then
    sm_setup
else
    cc_setup
    as_setup
    sm_setup
    cep_setup   
fi

# ------------------------------------------------
# Mapping domain/host names 
# ------------------------------------------------

cp -f /etc/hosts hosts.tmp

echo "$host_ip $sm_hostname	# stratos domain"	>> hosts.tmp
 
if [[ $profile = "sm" || $profile = "as" ]]; then
    echo "$sm_ip $sm_hostname	# stratos domain"	>> hosts.tmp
    echo "$cc_ip $cc_hostname	# cloud controller hostname"	>> hosts.tmp
fi

if [[ $profile = "sm" ]]; then
    echo "$as_ip $as_hostname	# auto scalar hostname"	>> hosts.tmp
fi

mv -f ./hosts.tmp /etc/hosts


# ------------------------------------------------
# Starting the servers
# ------------------------------------------------
echo 'Changing owner of '$stratos_path' to '$host_user:$host_user
chown $host_user:$host_user $stratos_path -R

echo "Apache Stratos setup has successfully completed"

if [[ $auto_start_servers != "true" ]]; then
    read -p "Do you want to start the servers [y/n]? " answer
    if [[ $answer != y ]] ; then
        exit 1
    fi
fi

echo "Starting the servers" >> $LOG

echo "Starting up servers. This may take time. Look at $LOG file for server startup details"

chown -R $host_user.$host_user $log_path
chmod -R 777 $log_path

export setup_dir=$PWD
su - $host_user -c "source $setup_path/conf/stratos-setup.conf;$setup_path/stratos-start-servers.sh -p\"$profile\" >> $LOG"

echo "Servers started. Please look at $LOG file for server startup details"
if [[ $profile == "default" || $profile == "sm" ]]; then
    echo "**************************************************************"
    echo "Management Console : https://$stratos_domain:$sm_https_port/console"
    echo "**************************************************************"
fi



