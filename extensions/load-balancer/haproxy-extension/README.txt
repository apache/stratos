================================================================================
                 Apache Stratos HAProxy Extension 4.0.0-SNAPSHOT
================================================================================


Apache Stratos (incubating) HAProxy extension is a load balancer extension for HAProxy.
It is an executable program which could manage the life-cycle of a HAProxy instance
according to topology updates received from Stratos Cloud Controller via the message broker.

Work Flow:
1. Wait for the complete topology event message to initialize the topology.
2. Configure and start an instance of HAProxy.
3. Listen to topology update messages.
4. Reload HAProxy instance with the new topology configuration.
5. Periodically publish statistics to Complex Event Processor (CEP).

Please refer INSTALL.txt for information on the installation process.


Thanks to Vaadin for HAProxyController implementation:
https://vaadin.com/license
http://dev.vaadin.com/browser/svn/incubator/Arvue/ArvueMaster/src/org/vaadin/arvue/arvuemaster/HAProxyController.java


Thank you for using Apache Stratos!
Apache Stratos Team
