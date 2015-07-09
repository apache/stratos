================================================================================
                                Apache Stratos
================================================================================

Welcome to the Apache Stratos distribution. This distribution includes Stratos Manager, Auto-scaler,
Complex Event Processor and Cloud Contoller components. In single-JVM mode all four components could be run
in single product and in multiple-JVM mode Stratos Manager, Auto-Scaler and Cloud Controller could be run using
this distribution with carbon profiles and Complex Event Processor needs to be run separately.

Stratos Manager (SM)
--------------------
Stratos Manager includes a comprehensive RESTful API for integration with external PaaS management interfaces for all
DevOps and user interaction.

Auto-scaler
-----------
Auto-scaler is responsible for the elasticity of all components of the system. It contains an embedded rule engine to
take fast and accurate decisions.

Complex Event Processor (CEP)
-----------------------------
Complex Event Processor does temporal (i.e., time-based) queries to analyze all the event streams that are being sent to
it and sends summarized information to the Auto-scaler. The event processing engine is an event aggregator/accumulator,
which takes lots of events and produces messages that Auto-scaler uses to make elasticity decisions in a more granular manner.

Cloud Controller (CC)
---------------------
Cloud Controller sends instructions via jClouds to the IaaS to create or destroy instances. It also listens to messages
from instances and updates the routing topology periodically. Topology updates fire messages on a topic that the LBs listen to.


Please refer below link for more information:
https://cwiki.apache.org/confluence/display/STRATOS/4.1.0+Architecture


Crypto Notice
=============

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

   Apacge Rampart   : http://ws.apache.org/rampart/
   Apache WSS4J     : http://ws.apache.org/wss4j/
   Apache Santuario : http://santuario.apache.org/
   Bouncycastle     : http://www.bouncycastle.org/


Thank you for using Apache Stratos!
The Stratos Team