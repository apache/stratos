# Apache Stratos LVS Extension

Apache Stratos LVS extension is a load balancer extension for LVS. It is an executable program
which can manage the life-cycle of a LVS instance according to the topology, composite application model,
tenant application signups and domain mapping information received from Stratos via the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure Keepalived
3. Listen to topology, application, application signup, domain mapping events.
4. Reload Keepalived instance with the new topology configuration.
5. Periodically publish statistics to Complex Event Processor (CEP).

## Statistics publishing
Set cep.stats.publisher.enabled to true in lvs-extension.sh file to enable statistics publishing.

## Installation
Please refer INSTALL.md for information on the installation process.
