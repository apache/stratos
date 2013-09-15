================================================================================
                        Apache Stratos Cloud Controller 3.0.0-incubating
================================================================================

Welcome to the Apache Stratos CC 3.0.0-incubating release.

Apache Stratos Cloud Controller plays a vital role in Stratos 2.0 and is open source and freely
distributed. Apache Stratos CC is available under the Apache Software License v2.0.

This is based on the revolutionary WSO2 Carbon [Middleware a' la carte]
framework. All the major features have been developed as pluggable Carbon
components.

Apache Stratos CC leverages the jclouds (http://jclouds.incubator.apache.org/) APIs and provides
a generic layer to communicate with different IaaSes. 

Key Features of Apache Stratos CC
========================

 * It acts as a bridge between application level and Infrastructure as a Service (IaaS) level via jclouds API.

 * It enables your system to scale across multiple IaaS providers.

 * It is the central location where the service topology resides.

 * It is responsible for periodically sharing up-to-date service topology among other Stratos 2.0 core services.

 * It supports hot updates and deployment of its configuration files.

 * It has inbuilt support for ,

        AWS EC2 IaaS provider

        Openstack Nova IaaS provider

        VMWare vCloud provider.

 * It enables you to cloud burst your system across multiple IaaS providers.

 * It allows you to easily plug an implementation of any IaaS provider support by jclouds.

 * It enables you to spawn new service instances, while associating a public IP automatically, to reduce the instance boot-up time.

 * It enables you to terminate an already started instance of a particular service cluster.

 * It can be configured to cover many scenarios, using it's well-thought-out configuration files.



System Requirements
=======================
1. Minimum memory - 1.5GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java SE Development Kit 1.6.24 or higher
4. The Management Console requires you to enable Javascript of the Web browser,
   with MS IE 7. In addition to JavaScript, ActiveX should also be enabled
   with IE. This can be achieved by setting your security level to
   medium or lower.
5. To compile and run the sample clients, an Ant version is required. Ant 1.7.0
   version is recommended
6. To build Apache Stratos CC from the Source distribution, it is necessary that you have
   JDK 1.6.24 version and Maven 3.0.4 or later


For more details see
    http://docs.wso2.org/wiki/display/Carbon401/Installation+Prerequisites

Installation & Running
==================================

1. Extract the apache-stratos-cc-3.0.0-incubating.zip and go to the extracted directory
2. Run the stratos.sh or stratos.bat as appropriate
3. Point your favourite browser to

    https://localhost:9443/carbon

4. Use the following username and password to login

    username : admin
    password : admin

   

Apache Stratos CC 3.0.0-incubating distribution directory structure
=============================================

	CARBON_HOME
		|-- bin <folder>
		|-- dbscripts <folder>
		|-- client-lib <folder>
		|-- lib <folder>
		|-- repository <folder>
		|   |-- components <folder>
		|   |-- conf <folder>
		|       |-- Advanced <folder>
		|   |-- database <folder>
		|   |-- deployment <folder>
			|-- server <folder>
			    |-- cartridges <folder>
			    |-- services <folder>		
		|   |-- logs <folder>
		|   |-- resources <folder>
		|       |-- security <folder>
		|-- tmp <folder>
		|-- LICENSE.txt <file>
		|-- INSTALL.txt <file>
		|-- README.txt <file>
		`-- release-notes.html <file>

    - bin
	  Contains various scripts .sh & .bat scripts

    - dbscripts
      Contains the SQL scripts for setting up the database on a variety of
      Database Management Systems, including H2, Derby, MSSQL, MySQL abd
      Oracle.

    - client-lib
      Contains required libraries for JMS,Event Clients

    - lib
      Contains the basic set of libraries required to start-up  Apache Stratos CC
      in standalone mode

    - repository
      The repository where cartridges and services are deployed in Apache Stratos CC.

        - components
          Contains OSGi bundles and configurations
      
        - conf
          Contains configuration files

          -Advanced
              Contains advanced configurations.
         
        - database
          Contains the database

        - deployment
          Contains Axis2 deployment details
          
        - logs
          Contains all log files created during execution

        - tenants
          Contains tenant details

    - resources
      Contains additional resources that may be required

        - security
          Contains security resources
          
    - tmp
      Used for storing temporary files, and is pointed to by the
      java.io.tmpdir System property

    - LICENSE.txt
      Apache License 2.0 under which Apache Stratos CC is distributed.

    - README.txt
      This document.

    - INSTALL.txt
      This document will contain information on installing Apache Stratos CC


Support
==================================

Any problem with this release can be reported to Apache Stratos mailing list
or in the JIRA issue tracker. If you are sending an email to the mailing
list make sure to add the [Apache Stratos] prefix to the subject.

Mailing list subscription:
    dev-subscribe@stratos.incubator.apache.org


Issue Tracker
==================================

Jira:
    https://issues.apache.org/jira/browse/stratos


Crypto Notice
==================================

   This distribution includes cryptographic software.  The country in
   which you currently reside may have restrictions on the import,
   possession, use, and/or re-export to another country, of
   encryption software.  BEFORE using any encryption software, please
   check your country's laws, regulations and policies concerning the
   import, possession, or use, and re-export of encryption software, to
   see if this is permitted.  See <http://www.wassenaar.org/> for more
   information.

   The U.S. Government Department of Commerce, Bureau of Industry and
   Security (BIS), has classified this software as Export Commodity
   Control Number (ECCN) 5D002.C.1, which includes information security
   software using or performing cryptographic functions with asymmetric
   algorithms.  The form and manner of this Apache Software Foundation
   distribution makes it eligible for export under the License Exception
   ENC Technology Software Unrestricted (TSU) exception (see the BIS
   Export Administration Regulations, Section 740.13) for both object
   code and source code.

   The following provides more details on the included cryptographic
   software:

   Apache Rampart   : http://ws.apache.org/rampart/
   Apache WSS4J     : http://ws.apache.org/wss4j/
   Apache Santuario : http://santuario.apache.org/
   Bouncycastle     : http://www.bouncycastle.org/


Thank you for using Apache Stratos!
The Stratos Team.

