Apahe Stratos version 3.0.0
---------------------------------

20th August 2013
Welcome to the Apache Stratos 3.0.0


Important
----------------------------------------

Apache Stratos could be installed on a single node or on multiple nodes. If you are installing on
multiple nodes copy stratos_installer to each node and update configuration parameters in conf/setup.conf accordingly.


How to install
----------------------------------------

1. Install following prerequisites:

    java    -jdk1.6.x
    git
    facter
    zip
    mysql-server
    Gitblits    

2. Build Apache Stratos from source:

   git clone https://git-wip-us.apache.org/repos/asf/incubator-stratos.git
   cd incubator-stratos
   mvn clean install

3. Copy cloud controller, stratos controller, elb, agent and cli packages to a folder inside stratos_installer. May be this could be called "stratos_installer/packages":
    
   apache-stratos-cc-<version>.zip 
   apache-stratos-sc-<version>.zip
   apache-stratos-elb-<version>.zip   
   apache-stratos-agent-<version>.zip
   apache-stratos-cli-<version>.zip    
  
4. Download MySql Java connector from http://dev.mysql.com/downloads and copy the jar file to the above packages folder.

5. Create and download the keys from IaaSs and store them on a secure location.

6. If Apache Stratos being setup in multiple nodes open up the security rules in IaaSs for the following ports (defined in ./conf/setup.conf)
   22, 443, 8280, 4103, 4100, agent_https_port, agent_http_port, elb_port, agent_clustering_port, sc_cluster_port, elb_cluster_port,
   cassandra_port, stratos_db_port and userstore_db_port.

7. Prepare cartridge images and upload them to the IaaSs. Please refer Apache Stratos documentation for more information on creating cartridge images.

8. Update ./conf/setup.conf and configure parameters. 

9. Run setup.sh as root to install.

    E.g -
    sudo ./setup.sh -p"<product-list>"
    <product-list> could be defined as "cc sc elb agent" or any other combination according to the deployment configuration.

    If you need to clean the setup
    clean.sh
    E.g. -
    sudo ./clean.sh -a<host-name> -b<host-username> -c<mysql-username> -d<mysql-password>

---------------------------------------------------------------------------
Thanks for using Apache Stratos
Apache Stratos Team
