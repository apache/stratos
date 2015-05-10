# Apache Stratos Nginx Extension

Apache Stratos Nginx extension is a load balancer extension for Nginx. It is an executable program
which can manage the life-cycle of a Nginx instance according to the topology, composite application model,
tenant application signups and domain mapping information received from Stratos via the message broker.

## How it works
1. Wait for the complete topology event message to initialize the topology.
2. Configure and start an instance of Nginx.
3. Listen to topology, application, application signup, domain mapping events.
4. Reload Nginx instance with the new topology configuration.

## Statistics publishing
Live statistics monitoring module is not available in Nginx basic version, need to recompile Nginx from source
with HttpStubStatusModule for enabling it. Therefore statistics publishing to CEP is not currently available in this
extension.
