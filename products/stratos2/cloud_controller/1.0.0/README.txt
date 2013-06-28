================================================================================
                        WSO2 Message Broker Server 2.0.1
================================================================================

Welcome to the WSO2 MB 2.0.1 release

WSO2 MB is a lightweight and easy-to-use Open Source Distributed Message Brokering
Server (MB) which is available under the Apache Software License v2.0.

This is based on the revolutionary WSO2 Carbon [Middleware a' la carte]
framework. All the major features have been developed as pluggable Carbon
components.

Key Features of WSO2 MB
==================================
WSO2 Message Broker brings messaging and eventing capabilities into your SOA framework.
The latest version of this product possesses following key features. All these features
can be used as standalone message broker or as a distributed message brokering system.

The underlying messaging framework of the WSO2 Message Broker is powered by Andes,
one of the distributed message brokering systems which is compatible with leading
Advanced Message Queuing Protocol (AMQP)(0-91)).Andes is one of the leading distributed
message brokering engine under apache license which is using Cassandra as its storage.

- JMS Queuing
- JMS Pub/Sub
- WS-Eventing


The Message Broker is compliant with the latest WS-Eventing specification.

The underlying JMS engine handles WS-Eventing/JMS synchronisation that enables exposing and
consuming your events using two different standard API's.



System Requirements
=======================
1. Minimum memory - 1GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java SE Development Kit 1.6.24 or higher
4. The Management Console requires you to enable Javascript of the Web browser,
   with MS IE 7. In addition to JavaScript, ActiveX should also be enabled
   with IE. This can be achieved by setting your security level to
   medium or lower.
5. To compile and run the sample clients, an Ant version is required. Ant 1.7.0
   version is recommended
6. To build WSO2 MB from the Source distribution, it is necessary that you have
   JDK 1.6.24 version and Maven 3.0.4 or later


For more details see
    http://docs.wso2.org/wiki/display/Carbon401/Installation+Prerequisites

Installation & Running
==================================

1. Extract the wso2mb-2.0.1.zip and go to the extracted directory
2. Run the wso2server.sh or wso2server.bat as appropriate
3. Point your favourite browser to

    https://localhost:9443/carbon

4. Use the following username and password to login

    username : admin
    password : admin

   

WSO2 MB 2.0.1 distribution directory structure
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
		|   |-- logs <folder>
		|   |-- tenants <folder>
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
      Contains the basic set of libraries required to start-up  WSO2 MB
      in standalone mode

    - repository
      The repository where services and modules deployed in WSO2 MB
      are stored.

        - components
          Contains OSGi bundles and configurations
      
        - conf
          Contains configuration files

          -Advanced
              Contains configuration files related to Andes and its storage
         
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
      Apache License 2.0 under which WSO2 MB is distributed.

    - README.txt
      This document.

    - INSTALL.txt
      This document will contain information on installing WSO2 MB



Support
==================================
WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support/

For more information on WSO2 MB, visit the WSO2 Oxygen Tank (http://wso2.org)

For more details and to take advantage of this unique opportunity please visit
http://wso2.com/support/

Thank you for your interest in WSO2 Message Broker.

Known Issues
==================================

 WSO2 Message Broker is compatible with AMQP 0-91 version only.

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

--------------------------------------------------------------------------------
(c) Copyright 2012 WSO2 Inc.

