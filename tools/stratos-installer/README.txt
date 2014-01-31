Apahe Stratos version 4.0.0-SNAPSHOT
-----------------------------

14th January 2014
Welcome to the Apache Stratos 4.0.0-SNAPSHOT


Important
-----------

Apache Stratos could be installed on a single node or on multiple nodes. When installing on
multiple nodes copy stratos_installer to each node and update configuration parameters in conf/setup.conf file accordingly.


How to Install
----------------

1. Install following prerequisites:

   java    -jdk1.6.x
   git
   zip
   mysql-server  

2. Build Apache Stratos from source:

   git clone https://git-wip-us.apache.org/repos/asf/incubator-stratos.git
   cd incubator-stratos
   mvn clean install

3. Copy cloud controller, stratos manager, autoscaler and cli packages to a desired location (this will be identified as stratos-pack-path):

   cp incubator-stratos/products/cloud-controller/modules/distribution/target/apache-stratos-cc-<version>.zip <stratos-pack-path>
   cp incubator-stratos/products/stratos-manager/modules/distribution/target/apache-stratos-manager-<version>.zip <stratos-pack-path> 
   cp incubator-stratos/products/autoscaler/modules/distribution/target/apache-stratos-autoscaler-<version>.zip <stratos-pack-path>   

4. Copy cep extensions jaf file to stratos-pack-path.

   cp incubator-stratos/extensions/cep/stratos-cep-extension/target/org.apache.stratos.cep.extension-<version>.jar <stratos-pack-path>

5. Download WSO2 Message Broker binary distribution from http://wso2.com/products/message-broker/ and copy it to stratos-pack-path. Here you could use any preferred 
   message broker product which supports AMQP.
   Message broker is used for communication of Stratos products,here we show how to use WSO2 MB for this.

6. Download WSO2 Complex Event Processor binary distribution from http://wso2.com/products/complex-event-processor/ and copy it to stratos-pack-path.

7. Download MySql Java connector from http://dev.mysql.com/downloads and copy the jar file to stratos-pack-path.

8. Create and download keys from IaaSs and store them on a secure location.

9. If Apache Stratos being setup in multiple nodes open up the security rules in IaaSs for ports which are used in cloud controller, stratos manager, autoscaler,
   WSO2 Message Broker and WSO2 Complex Event Processor as well as stratos_db_port, userstore_db_port ports (defined in ./conf/setup.conf file).

10. Either download pre-built cartridge images from Apache Stratos website or create your own cartridges. Please refer Apache Stratos documentation 
   for more information on creating cartridge images. For Amazon EC2, you could find pre-built PHP, MySQL and Tomcat cartridges published in Amazon EC2
   AMI image repository.

11. Update ./conf/setup.conf and configure settings.

12. Run setup.sh as root to install.

    sudo ./setup.sh -p "<product-list>"
    <product-list> could be defined as "cc sm as mb cep" or any other combination according to the deployment configuration.
    Example:
    sudo ./setup.sh -p "all"

    If you need to clean the setup run bellow command:
    sudo ./clean.sh -u <mysql-username> -p <mysql-password>


---------------------------------------------------------------------------
Thanks for using Apache Stratos
Apache Stratos Team
