================================================================================
                        WSO2 Stratos Agent 1.0.1
================================================================================

Welcome to the WSO2 Stratos Agent 1.0.1 release

WSO2 Stratos Agent provides services to Non-Carbon Cartridges and it helps
to communicate the status of Cartridge Instances with WSO2 Stratos Controller.

System Requirements
==================================

1. Minimum memory - 1GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. Java SE Development Kit 1.6.0_24 or higher


WSO2 Stratos Agent 1.0.1 distribution directory structure
=========================================================

    CARBON_HOME
	|- bin <folder>
	|- lib <folder>
	|- repository <folder>
	|- tmp <folder>
	|- LICENSE.txt <file>
	|- README.txt <file>
	|- INSTALL.txt <file>		
	|- release-notes.html <file>

    - bin
	  Contains various scripts, .sh & .bat files

    - dbscripts
    Contains all the database scripts

    - lib
	  Contains the basic set of libraries required to startup Stratos Agent
	  in standalone mode

    - repository
	  The repository where services and modules deployed in WSO2 Stratos Agent
	  are stored. In addition to this, the components directory inside the
	  repository directory contains the carbon runtime and the user added
	  jar files including mediators, third party libraries etc. All
	  global and LB specific configuration files, generated log files
	  and other deployed artifacts are also housed under this directory.

    - tmp
	  Used for storing temporary files, and is pointed to by the
	  java.io.tmpdir System property

    - LICENSE.txt
	  Apache License 2.0 and the relevant other licenses under which
	  WSO2 Stratos Agent is distributed.

    - README.txt
	  This document.

    - INSTALL.txt
      This document will contain information on installing WSO2 Stratos Agent

Support
==================================

WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support/

For more information on WSO2 Stratos Agent, visit the WSO2 Oxygen Tank (http://wso2.org)

Issue Tracker
==================================

  https://wso2.org/jira/browse/CARBON
  https://wso2.org/jira/browse/SPI

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
(c) Copyright 2013 WSO2 Inc.
