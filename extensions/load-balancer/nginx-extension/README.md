# Apache Stratos Nginx Load Balancer Extension

Stratos Nginx load balancer extension can be used for integrating Nginx with Stratos. It provides means of
configuring the nginx according to the topology, composite application model, tenant information, application
signups and domain mappings.

## How it works
* The load balancer extension subscribe to the message broker and receives above information from Stratos.
* Afterwards it generates Nginx configuration and start an instance of Nginx with the generated configuration file.
* Once a change in the above data set is detected a new configuration will be generated and the load balancer will be reloaded.