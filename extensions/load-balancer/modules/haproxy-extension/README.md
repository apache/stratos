# Apache Stratos HAProxy Extension

Apache Stratos HAProxy extension is a load balancer extension for HAProxy. It is an executable program
which can manage the life-cycle of a HAProxy instance according to the topology, composite application model,
tenant application signups and domain mapping information received from Stratos via the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure and start an instance of HAProxy.
3. Listen to topology, application, application signup, domain mapping events.
4. Reload HAProxy instance with the new topology configuration.
5. Periodically publish statistics to Complex Event Processor (CEP).

## Installation
Please refer INSTALL.md for information on the installation process.

Thanks to Vaadin for HAProxyController implementation:
https://vaadin.com/license
http://dev.vaadin.com/browser/svn/incubator/Arvue/ArvueMaster/src/org/vaadin/arvue/arvuemaster/HAProxyController.java

