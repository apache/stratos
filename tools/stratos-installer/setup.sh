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

cc="false"
elb="false"
agent="false"
sc="false"
product_list="cc;elb;agent;sc"
enable_internal_git=false

function help {
    echo ""
    echo "Usage:"
    echo "setup.sh -u <host username> -p \"<product list>\""
    echo "Example:"
    echo "sudo ./setup.sh -p \"cc elb\""
    echo "sudo ./setup.sh -p \"all\""
    echo ""
    echo "-u: <host username> The login user of the host."
    echo "-p: <product list> Apache Stratos products to be installed on this node. Provide one or more names of the servers."
    echo "    The available servers are cc, elb, agent, sc or all. 'all' means you need to setup all servers in this machine. Default is all"
    echo "-g: <enable_internal_git> true|false Whether enable internal git repo for Stratos2. Default is false"
    echo ""
}

while getopts p:g: opts
do
  case $opts in
    p)
        product_list=${OPTARG}
        ;;
    g)
        enable_internal_git=${OPTARG}
        ;;
    *)
        help
        exit 1
        ;;
  esac
done


arr=$(echo $product_list | tr " " "\n")

for x in $arr
do
    if [[ $x = "cc" ]]; then
        cc="true"
    fi
    if [[ $x = "elb" ]]; then
        elb="true"
    fi
    if [[ $x = "agent" ]]; then
        agent="true"
    fi
    if [[ $x = "sc" ]]; then
        sc="true"
    fi
    if [[ $x = "all" ]]; then
        cc="true"
        elb="true"
        agent="true"
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

echo "user provided in conf/setup.conf is $host_user. If you want to provide some other username please specify it at the prompt."
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

    if [[ -z $userstore_db_hostname ]]; then
        userstore_db_hostname=""
    fi
    if [[ -z $sc_hostname ]]; then
        sc_hostname=$hostname
    fi
    if [[ -z $stratos_foundation_db_hostname ]]; then
        stratos_foundation_db_hostname=$hostname
    fi
    if [[ -z $agent_hostname ]]; then
        agent_hostname=$hostname
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
    if [[ -z $elb_hostname ]]; then
        elb_hostname=$hostname
    fi

    if [[ ( -z $hostip ) ]]; then
        hostip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z $hostip ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ( -z $elb_ip ) ]]; then
        elb_ip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z elb_ip ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ ( -z $agent_ip ) ]]; then
        agent_ip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
        if [[ ( -z $agent_ip ) ]]; then
            helpsetup
            exit 1
        fi
    fi


    if [[ -z $git_ip ]]; then
        git_ip=$hostip
    fi

    if [[ $sc = "true" ]]; then
        if [[ $enable_internal_git = "true" ]]; then
            if [[ -z $git_user ]]; then
                echo "Please specify the git user, because it will be needed to create an internal git repo"
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

    if [[ $elb = "true" ]]; then
        if [[ ( -z $hostname || -z $elb_path ) ]]; then
            helpsetup
            exit 1
        fi
    fi

    if [[ $agent = "true" ]]; then
        if [[ ( -z $hostname || -z $agent_path ) ]]; then
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
if [[ $elb = "true" ]]; then
    if [[ ! -d $elb_path ]]; then
        unzip $elb_pack -d $stratos_path
    fi
fi
if [[ $cc = "true" ]]; then
    if [[ ! -d $cc_path ]]; then
        unzip $cc_pack -d $stratos_path
    fi
fi
if [[ $agent = "true" ]]; then
    if [[ ! -d $agent_path ]]; then
        unzip $agent_pack -d $stratos_path
    fi
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
    
    popd # sc_path


    # Database Configuration
    # -----------------------------------------------
    echo "Create and configure MySql Databases" >> $LOG

    echo "Creating userstore database"
    mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/userstore.sql
    
    echo "Creating stratos_foundation database"
    mysql -u$stratos_foundation_db_user -p$stratos_foundation_db_pass < $resource_path/stratos_foundation.sql


    echo "End configuring the SC"
fi #End SC server installation


# Setup CC
# ------------------------------------------------
if [[ $cc = "true" ]]; then
    echo "Setup CC" >> $LOG
    echo "Configuring the Cloud Controller"
    echo "Creating payload directory ... " >> $LOG
    if [[ ! -d $cc_path/repository/resources/payload ]]; then
        mkdir -p $cc_path/repository/resources/payload
    fi

    #cp -f ./config/cc/bin/stratos.sh $cc_path/bin/
    cp -f ./config/cc/repository/conf/cloud-controller.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/

    #MB specific file copying
    #cp -f ./config/cc/repository/conf/advanced/qpid-virtualhosts.xml $cc_path/repository/conf/advanced/
    #cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/
    #End MB specific file copying


    # Setup IaaS providers
    # ------------------------------------------------
    # <iaasProviders>
    # <!--iaasProvider type="ec2" name="ec2 specific details">
    #      <className>org.wso2.carbon.stratos.cloud.controller.iaases.AWSEC2Iaas</className>
    #      <provider>aws-ec2</provider>
    #      <identity svns:secretAlias="elastic.scaler.openstack.identity">dhsaghfdal</identity>
    #      <credential svns:secretAlias="elastic.scaler.openstack.credential">jdkjaskd</credential>
    #      <scaleUpOrder>1</scaleUpOrder>
    #      <scaleDownOrder>2</scaleDownOrder>
    #      <property name="jclouds.ec2.ami-query" value="owner-id=XX-XX-XX;state=available;image-type=machine"/>
    #      <property name="availabilityZone" value="us-east-1c"/>
    #      <property name="securityGroups" value="manager,cep,mb,default"/>
    #      <property name="instanceType" value="m1.large"/>
    #      <property name="keyPair" value="nirmal-key"/>
    #      <imageId>us-east-1/ami-52409a3b</imageId>
    # </iaasProvider-->
    #      
    # <iaasProvider type="openstack" name="openstack specific details">
    #      <className>org.wso2.carbon.stratos.cloud.controller.iaases.OpenstackNovaIaas</className>
    #      <provider>openstack-nova</provider>
    #      <identity svns:secretAlias="cloud.controller.openstack.identity">demo:demo</identity>
    #      <credential svns:secretAlias="cloud.controller.openstack.credential">openstack</credential>
    #      <property name="jclouds.endpoint" value="http://192.168.16.20:5000/" />
    #      <property name="jclouds.openstack-nova.auto-create-floating-ips" value="false"/>
    #      <property name="jclouds.api-version" value="2.0/" />
    #      <scaleUpOrder>2</scaleUpOrder>
    #      <scaleDownOrder>3</scaleDownOrder>
    #      <property name="X" value="x" />
    #      <property name="Y" value="y" />
    #      <imageId>nova/dab37f0e-cf6f-4812-86fc-733acf22d5e6</imageId>
    # </iaasProvider>
    # </iaasProviders>

    if [[ $ec2_provider_enabled = "true" ]]; then
        ./ec2.sh
    fi
    if [[ $openstack_provider_enabled = "true" ]]; then
        ./openstack.sh
    fi

    pushd $cc_path
    
    cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
    cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/cloud-controller.xml

    echo "Set settings in cc/repository/conf/carbon.xml" >> $LOG
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@CC_PORT_OFFSET@$cc_port_offset@g" > repository/conf/carbon.xml
    #Before starting sc delete rm -rf tmp/ at mb root folder
    rm -rf ./tmp

    popd #cc_path
    echo "End configuring the Cloud Controller"
fi


# Setup ELB
# -------------------------------------------------------------    
if [[ $elb = "true" ]]; then
    echo "Setup ELB" >> $LOG
    echo "Configuring the ELB"
    #cp -f ./config/elb/bin/stratos.sh $elb_path/bin/
    cp -f ./config/elb/repository/conf/loadbalancer.conf $elb_path/repository/conf/
    cp -f ./config/elb/repository/conf/axis2/axis2.xml $elb_path/repository/conf/axis2/
    cp -f ./config/elb/repository/conf/datasources/master-datasources.xml $elb_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $elb_path/repository/components/lib/

    pushd $elb_path
    #If autoscaling enabled
    echo "Set CC host and port in repository/conf/loadbalancer.conf" >> $LOG
    # autoscaler_service_epr  https://CC_HOSTNAME:CC_PORT/services/CloudControllerService;
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@CC_HOSTNAME:CC_PORT@$cc_hostname:$cc_https_port@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@ENABLE_AUTOSCALER@$enable_autoscaler@g" > repository/conf/loadbalancer.conf
    
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@ENABLE_EMBEDDED_AUTOSCALER@$enable_embedded_autoscaler@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$mb_hostname:$mb_listen_port@g" > repository/conf/loadbalancer.conf

    echo "Set hostname of the machine where elb run, in repository/conf/axis2/axis2.xml" >> $LOG
    #<!--parameter name="localMemberHost">ELB_HOSTNAME</parameter-->
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/axis2/axis2.xml


    echo "Set hostname of the machine where elb run, in repository/conf/etc/jmx.xml" >> $LOG
    cp -f repository/conf/etc/jmx.xml repository/conf/etc/jmx.xml.orig
    cat repository/conf/etc/jmx.xml.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/etc/jmx.xml


    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f ./repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_HOSTNAME@$userstore_db_hostname@g" | sed -e "s@USERSTORE_DB_PORT@$userstore_db_port@g"| sed -e "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g"|sed -e "s@USERSTORE_DB_USER@$userstore_db_user@g" |sed -e "s@USERSTORE_DB_PASS@$userstore_db_pass@g" > repository/conf/datasources/master-datasources.xml

    popd #elb_path
    echo "End configuring the ELB"
fi


# Setup Agent
# -------------------------------------------------------------    
if [[ $agent = "true" ]]; then
    echo "Setup Agent" >> $LOG
    echo "Configuring the Agent"
    cp -f ./config/agent/repository/conf/agent.properties $agent_path/repository/conf/
    cp -f ./config/agent/repository/conf/carbon.xml $agent_path/repository/conf/
    pushd $agent_path

    rm -rf registrants/

    #Set agent host and ELb host in repository/conf/agent.properties.
    #Note that loadBalancerDomain=wso2.carbon.lb.domain should be same as elb/repository/conf/axis2/axis2.xml
    #<parameter name="domain">wso2.carbon.lb.domain</parameter>
    echo "Set agent hostname in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@AGENT_HOSTNAME@$agent_hostname@g" > repository/conf/agent.properties

    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@AGENT_CLUSTERING_PORT@$agent_clustering_port@g" > repository/conf/agent.properties

    echo "Set ELB hostname in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/agent.properties
    
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@ELB_CLUSTER_PORT@$elb_cluster_port@g" > repository/conf/agent.properties

    echo "Set SC_PATH in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_PATH@$sc_path@g" > repository/conf/agent.properties

    echo "Set SC_HOST in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_HOSTNAME@$sc_hostname@g" > repository/conf/agent.properties

    echo "Set SC_HTTPS_PORT in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_HTTPS_PORT@$sc_https_port@g" > repository/conf/agent.properties

    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@AGENT_PORT_OFFSET@${agent_port_offset}@g" > repository/conf/carbon.xml

    popd #agent_path
    echo "End configuring the Agent"
fi


# Configure cartridges
# ---------------------------------------------------------
if [[ $openstack_provider_enabled = "true" ]]; then
    ./openstack-cartridge.sh
fi
if [[ $ec2_provider_enabled = "true" ]]; then
    ./ec2-cartridge.sh
fi


# Map domain/host names of each product 
# ---------------------------------------------------------------------------- 

echo 'Updating /etc/hosts file with domain names' 
cp -f /etc/hosts hosts.tmp 
 
echo "$ip $DOMAIN # stratos domain" >> hosts.tmp 
echo "$ip mb.$DOMAIN # message broker hostname" >> hosts.tmp 
echo "$ip cc.$DOMAIN # cloud controller hostname" >> hosts.tmp 
echo "$ip sc.$DOMAIN # stratos controller hostname" >> hosts.tmp 
echo "$ip elb.$DOMAIN # elastic load balancer hostname" >> hosts.tmp 
echo "$ip agent.$DOMAIN # agent hostname" >> hosts.tmp 
 
mv -f ./hosts.tmp /etc/hosts

# Starting the servers
# ---------------------------------------------------------
echo 'Changing owner of '$stratos_path' to '$host_user:$host_user
chown $host_user:$host_user $stratos_path -R

echo "Apache Stratos setup has successfully completed"

read -p "Do you want to start the servers [y/n]? " answer
if [[ $answer != y ]] ; then
    exit 1
fi

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

